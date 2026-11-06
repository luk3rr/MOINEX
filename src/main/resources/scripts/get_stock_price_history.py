#!/usr/bin/env python

# Filename: get_stock_price_history.py
# Created on: February 17, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
Usage:
  get_stock_price_history.py <symbol> <start_date> [end_date] [specific_dates_json]
  get_stock_price_history.py --batch <json>

The single form prints {"symbol", "currency", "conversion_rate", "prices": [...]} for one symbol and, on a
failure, {"error", "error_kind"} with exit code 1.

The batch form reads a JSON array of {"symbol", "start", "end", "currency"?, "dates"?} and prints
{"results": {symbol: <the object above> | {"error", "error_kind"}}}. One download serves the whole chunk, so
30 tickers cost a handful of requests instead of 30 processes. A symbol that failed does not hide the others.

A symbol whose currency is not given has it read from yfinance info; if that fails the symbol gets an error:
a currency is never assumed and a conversion rate is never guessed, since either would store wrong prices.
Dates are YYYY-MM-DD; the end date is inclusive. The range asked for is the only range requested, with one
extra 15 day request when it ends within 5 days of today and came back empty (long weekends and holidays).
"""

import json
import os
import sys
import time
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import yfinance as yf

import yf_resilience as res

res.use_explicit_dns_on_windows()

DEFAULT_CURRENCY = res.DEFAULT_CURRENCY

IS_WINDOWS = sys.platform == "win32"
BATCH_SIZE = 8
INTER_BATCH_DELAY = 2.0
RECENT_END_DAYS = 5
FALLBACK_PERIOD = "15d"


def parse_date(text):
    return datetime.strptime(text, "%Y-%m-%d").date()


def download_history(symbols, **kwargs):
    """
    One yf.download() call for the symbols, retried on a rate limit. yf.download() does not raise when a
    symbol fails, so the messages it logs are checked: a rate limit among them raises.
    Returns (data, messages).
    """
    def attempt():
        with res.capture_yfinance_errors() as messages:
            data = yf.download(symbols, progress=False, auto_adjust=True, threads=not IS_WINDOWS, **kwargs)
        for message in messages:
            if res.is_rate_limit(message):
                raise Exception(f"rate limit: {message}")
        return data, list(messages)

    return res.retry_call(attempt, ",".join(symbols))


def close_series(data, symbol):
    """The closing prices of a symbol from a yf.download result, whatever shape the columns have."""
    if data is None or data.empty or "Close" not in data.columns:
        return None
    close = data["Close"]
    if hasattr(close, "columns"):
        if symbol in close.columns:
            return close[symbol].dropna()
        if len(close.columns) == 1:
            return close.iloc[:, 0].dropna()
        return None
    return close.dropna()


def slice_range(series, start, end):
    """The closes between start and end (both inclusive); None when nothing is left."""
    if series is None or series.empty:
        return None
    kept = series[(series.index.date >= start) & (series.index.date <= end)]
    return kept if not kept.empty else None


def fetch_closes(items):
    """
    Downloads the closes of every item with one request per chunk of BATCH_SIZE symbols. After a rate limit no
    further chunk is requested.

    @return: ({symbol: closes within its own range}, {symbol: error payload})
    """
    closes = {}
    errors = {}
    chunks = [items[i:i + BATCH_SIZE] for i in range(0, len(items), BATCH_SIZE)]
    rate_limited = None

    for index, chunk in enumerate(chunks):
        symbols = [item["symbol"] for item in chunk]

        if rate_limited is not None:
            for symbol in symbols:
                errors[symbol] = res.error_payload(rate_limited, kind=res.KIND_RATE_LIMIT)
            continue

        if index > 0:
            time.sleep(INTER_BATCH_DELAY)

        start = min(item["start"] for item in chunk)
        end = max(item["end"] for item in chunk)

        try:
            data, messages = download_history(symbols, start=start.isoformat(), end=end.isoformat())
        except Exception as e:
            print(f"[WARN] Chunk {index + 1}/{len(chunks)} download failed: {e}", file=sys.stderr)
            if res.is_rate_limit(e):
                rate_limited = e
            for symbol in symbols:
                errors[symbol] = res.error_payload(e)
            continue

        for item in chunk:
            symbol = item["symbol"]
            series = slice_range(close_series(data, symbol), item["start"], item["end"])
            if series is not None:
                closes[symbol] = series
            else:
                about = res.messages_about(symbol, messages)
                if about:
                    errors[symbol] = res.error_payload("; ".join(about))

    missing = [item for item in items if item["symbol"] not in closes and item["symbol"] not in errors]
    recent = [item for item in missing if 0 <= (datetime.now().date() - item["end"]).days <= RECENT_END_DAYS]

    if recent and rate_limited is None:
        symbols = [item["symbol"] for item in recent]
        try:
            data, _ = download_history(symbols, period=FALLBACK_PERIOD)
            for item in recent:
                series = slice_range(close_series(data, item["symbol"]), item["start"], item["end"])
                if series is not None:
                    closes[item["symbol"]] = series
        except Exception as e:
            print(f"[WARN] Fallback download failed: {e}", file=sys.stderr)
            for item in recent:
                errors[item["symbol"]] = res.error_payload(e)

    for item in items:
        symbol = item["symbol"]
        if symbol not in closes and symbol not in errors:
            errors[symbol] = res.error_payload(
                f"No data found for {symbol} in the date range {item['start']} to {item['end']}",
                kind=res.KIND_INVALID_SYMBOL,
            )

    return closes, errors


def resolve_currency(item):
    """The currency the app passed, or the one read from yfinance info (an error payload when unknown)."""
    if item.get("currency"):
        return item["currency"].upper(), None
    try:
        currency, _ = res.retry_call(lambda: res.lookup_currency_and_website(item["symbol"], yf), item["symbol"])
        return currency, None
    except Exception as e:
        kind = res.KIND_RATE_LIMIT if res.is_rate_limit(e) else res.KIND_CURRENCY_UNKNOWN
        return None, res.error_payload(e, kind=kind)


def build_prices(closes, specific_dates, rate):
    """Month-end closes plus the specific dates asked for (all of them when none were asked), converted."""
    month_ends = set()
    for _, group in closes.groupby(closes.index.to_period("M")):
        month_ends.add(group.index[-1].date())

    wanted = set()
    for text in specific_dates or []:
        try:
            wanted.add(parse_date(text))
        except (TypeError, ValueError):
            pass

    prices = []
    for index, close in closes.items():
        day = index.date()
        is_month_end = day in month_ends
        if is_month_end or day in wanted or not specific_dates:
            prices.append({"date": day.strftime("%Y-%m-%d"), "price": float(close) * rate, "is_month_end": is_month_end})
    return prices


def build_results(items):
    """The result object of every item, keyed by symbol."""
    closes, errors = fetch_closes(items)
    results = {}

    for item in items:
        symbol = item["symbol"]
        if symbol in errors:
            results[symbol] = errors[symbol]
            continue

        currency, failure = resolve_currency(item)
        if failure is not None:
            results[symbol] = failure
            continue

        try:
            rate = res.get_conversion_rate(currency)
        except Exception as e:
            results[symbol] = res.error_payload(f"Conversion rate from {currency} unavailable: {e}")
            continue

        results[symbol] = {
            "symbol": symbol,
            "currency": currency,
            "conversion_rate": rate,
            "prices": build_prices(closes[symbol], item.get("dates"), rate),
        }

    return results


def parse_batch(text):
    items = []
    for raw in json.loads(text):
        items.append({
            "symbol": raw["symbol"],
            "start": parse_date(raw["start"]),
            "end": parse_date(raw["end"]) if raw.get("end") else datetime.now().date(),
            "currency": raw.get("currency"),
            "dates": raw.get("dates"),
        })
    return items


def main():
    if len(sys.argv) < 3:
        print("Usage: python get_stock_price_history.py <symbol> <start_date> [end_date] [specific_dates_json]")
        print("       python get_stock_price_history.py --batch <json>")
        sys.exit(1)

    if sys.argv[1] == "--batch":
        try:
            items = parse_batch(sys.argv[2])
        except Exception as e:
            print(json.dumps(res.error_payload(f"Invalid batch: {e}", kind=res.KIND_ERROR)))
            sys.exit(1)
        print(json.dumps({"results": build_results(items)}))
        return

    specific_dates = []
    if len(sys.argv) > 4:
        try:
            specific_dates = json.loads(sys.argv[4])
        except Exception:
            specific_dates = []

    try:
        item = {
            "symbol": sys.argv[1],
            "start": parse_date(sys.argv[2]),
            "end": parse_date(sys.argv[3]) if len(sys.argv) > 3 else datetime.now().date(),
            "currency": None,
            "dates": specific_dates,
        }
        result = build_results([item])[item["symbol"]]
    except Exception as e:
        result = res.error_payload(e)

    print(json.dumps(result))
    if "error" in result:
        print(f"[ERROR] error_kind={result['error_kind']}: {result['error']}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
