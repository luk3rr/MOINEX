#!/usr/bin/env python

# Filename: get_stock_price.py
# Created on: January 12, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
Usage: get_stock_price.py SYMBOL[:CURRENCY] ...

Prints one JSON object keyed by symbol. A symbol that worked is {"price": <in BRL>, "currency": <of the
quote>, "website": <only when it had to be looked up>}. A symbol that failed is {"error": ..., "error_kind": ...}.

When the currency is given (SYMBOL:USD) the script never calls yfinance info for that symbol: the app stores
the currency once and passes it on the next runs. Without a currency the script reads it, and the website,
from info. If that fails the symbol gets an error and no price: a currency is never assumed and a
conversion rate is never guessed, since either would store a wrong price.
"""

import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import yfinance as yf

import yf_resilience as res

res.use_explicit_dns_on_windows()

DEFAULT_CURRENCY = res.DEFAULT_CURRENCY

IS_WINDOWS = sys.platform == "win32"
MAX_INFO_WORKERS = 1 if IS_WINDOWS else 5
BATCH_SIZE = 8
INTER_BATCH_DELAY = 2.0


def parse_arguments(arguments):
    """
    Splits SYMBOL[:CURRENCY] arguments into the ordered list of symbols and a {symbol: currency} map.
    """
    symbols = []
    currencies = {}
    for argument in arguments:
        symbol, _, currency = argument.partition(":")
        if symbol and symbol not in symbols:
            symbols.append(symbol)
        if symbol and currency:
            currencies[symbol] = currency.upper()
    return symbols, currencies


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


def download_chunk(chunk):
    """
    Downloads the last days of a chunk of symbols and returns (data, error messages yfinance logged).
    yf.download() does not raise on a failure, so the logged messages are checked: a rate limit among
    them raises, for the caller to retry. An empty answer without a rate limit is not an error: those
    symbols just have no data.
    """
    with res.capture_yfinance_errors() as messages:
        data = yf.download(chunk, period="5d", progress=False, auto_adjust=True, threads=not IS_WINDOWS)

    for message in messages:
        if res.is_rate_limit(message):
            raise Exception(f"rate limit: {message}")
    return data, list(messages)


def fetch_prices_batch(symbols):
    """
    Fetches the last close of each symbol with chunked yf.download() calls, INTER_BATCH_DELAY apart. After a
    rate limit no further chunk is requested, so a blocked run costs a handful of requests instead of dozens.

    @return: ({symbol: price in the quote currency}, {symbol: error payload})
    """
    prices = {}
    errors = {}
    chunks = [symbols[i:i + BATCH_SIZE] for i in range(0, len(symbols), BATCH_SIZE)]
    rate_limited = None

    for index, chunk in enumerate(chunks):
        if rate_limited is not None:
            for symbol in chunk:
                errors[symbol] = res.error_payload(rate_limited, kind=res.KIND_RATE_LIMIT)
            continue

        if index > 0:
            time.sleep(INTER_BATCH_DELAY)

        try:
            data, messages = res.retry_call(lambda: download_chunk(chunk), f"chunk {index + 1}/{len(chunks)}")
        except Exception as e:
            print(f"[WARN] Chunk {index + 1}/{len(chunks)} download failed: {e}", file=sys.stderr)
            if res.is_rate_limit(e):
                rate_limited = e
            for symbol in chunk:
                errors[symbol] = res.error_payload(e)
            continue

        for symbol in chunk:
            series = close_series(data, symbol)
            if series is not None and not series.empty:
                prices[symbol] = float(series.iloc[-1])
            elif res.messages_about(symbol, messages):
                errors[symbol] = res.error_payload("; ".join(res.messages_about(symbol, messages)))
            else:
                errors[symbol] = res.error_payload(
                    f"No price data available for {symbol}", kind=res.KIND_INVALID_SYMBOL
                )

    return prices, errors


def lookup_symbol(symbol):
    """
    Reads currency and website of a symbol with a single info call (also retried on a rate limit).
    Returns (symbol, (currency, website)) or (symbol, error payload).
    """
    try:
        return symbol, res.retry_call(lambda: res.lookup_currency_and_website(symbol, yf), symbol)
    except Exception as e:
        kind = res.KIND_RATE_LIMIT if res.is_rate_limit(e) else res.KIND_CURRENCY_UNKNOWN
        return symbol, res.error_payload(e, kind=kind)


def build_result(symbols, prices, errors, currencies, lookups):
    """
    Turns the prices into the output. Only a symbol with a price, a known currency and a conversion
    rate gets a price; every other symbol gets its error.
    """
    result = {}
    for symbol in symbols:
        if symbol in errors:
            result[symbol] = errors[symbol]
            continue

        currency = currencies.get(symbol)
        website = None
        if currency is None:
            looked_up = lookups.get(symbol)
            if isinstance(looked_up, dict):
                result[symbol] = looked_up
                continue
            currency, website = looked_up

        try:
            rate = res.get_conversion_rate(currency)
        except Exception as e:
            result[symbol] = res.error_payload(f"Conversion rate from {currency} unavailable: {e}")
            continue

        result[symbol] = {"price": prices[symbol] * rate, "currency": currency}
        if website:
            result[symbol]["website"] = website

    return result


def main():
    if len(sys.argv) < 2:
        print("Usage: python get_stock_price.py <symbol[:currency]> ... <symbol[:currency]>")
        sys.exit(1)

    symbols, currencies = parse_arguments(sys.argv[1:])

    try:
        prices, errors = fetch_prices_batch(symbols)
    except Exception as e:
        print(f"[WARN] Price fetch failed: {e}", file=sys.stderr)
        prices, errors = {}, {symbol: res.error_payload(e) for symbol in symbols}

    needs_lookup = [s for s in symbols if s in prices and s not in currencies]
    lookups = {}
    if needs_lookup:
        with ThreadPoolExecutor(max_workers=MAX_INFO_WORKERS) as executor:
            lookups = dict(executor.map(lookup_symbol, needs_lookup))

    print(json.dumps(build_result(symbols, prices, errors, currencies, lookups)))


if __name__ == "__main__":
    main()
