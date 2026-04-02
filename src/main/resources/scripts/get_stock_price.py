#!/usr/bin/env python

# Filename: get_stock_price.py
# Created on: January 12, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
import yfinance as yf
import requests
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.parse import urlparse

DEFAULT_CURRENCY = "BRL"

EXCHANGE_API_URL_BASE = "https://api.exchangerate-api.com/v4/latest"

TIMEOUT_SECONDS = 20

# Configure yfinance session with custom headers
from requests import Session

# Create a session with custom headers to avoid being blocked
session = Session()
session.headers.update({
    'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.5',
    'Accept-Encoding': 'gzip, deflate',
    'DNT': '1',
    'Connection': 'keep-alive',
    'Upgrade-Insecure-Requests': '1'
})

RETRY_MAX_ATTEMPTS = 3
RETRY_INITIAL_DELAY = 1.0
RETRY_MULTIPLIER = 1.5
RETRY_KEYWORDS = ("429", "rate limit", "too many requests")

MAX_INFO_WORKERS = 5
BATCH_SIZE = 8
INTER_BATCH_DELAY = 2.0


def retry_call(fn, symbol):
    """
    Calls fn(), retrying on rate-limit errors with exponential backoff.
    Writes retry status to stderr. Returns the result or raises on exhaustion.
    """
    delay = RETRY_INITIAL_DELAY
    for attempt in range(1, RETRY_MAX_ATTEMPTS + 1):
        try:
            return fn()
        except Exception as e:
            # Check both exception type and message for rate limiting
            error_str = str(e).lower()
            error_type = type(e).__name__
            is_rate_limit = (
                any(k in error_str for k in RETRY_KEYWORDS)
                or "ratelimit" in error_type.lower()
            )
            if not is_rate_limit or attempt == RETRY_MAX_ATTEMPTS:
                raise
            print(
                f"[RETRY] {symbol} attempt {attempt + 1}/{RETRY_MAX_ATTEMPTS} "
                f"after {delay:.0f}s delay: {e}",
                file=sys.stderr,
            )
            time.sleep(delay)
            delay *= RETRY_MULTIPLIER


def get_conversion_rate(
    from_currency: str, to_currency: str = DEFAULT_CURRENCY
) -> float:
    """
    Convert the amount from one currency to another using the exchange rate API

    @param from_currency: The currency to convert from
    @param to_currency: The currency to convert to
    @return: The conversion rate from the source currency to the target currency
    """
    # Case the source or target currency is 'USX', convert to dollar (USD) first
    # USX is a virtual currency that represents cents of dollar
    if from_currency == "USX":
        base_rate = get_conversion_rate("USD", to_currency)
        return base_rate / 100
    elif to_currency == "USX":
        base_rate = get_conversion_rate(from_currency, "USD")
        return base_rate * 100

    # Get the exchange rates from the API
    url = f"{EXCHANGE_API_URL_BASE}/{from_currency}"
    response = requests.get(url, timeout=TIMEOUT_SECONDS)

    if response.status_code == 200:
        data = response.json()

        if to_currency in data["rates"]:
            return data["rates"][to_currency]
        else:
            raise ValueError(
                f"Currency {to_currency} or {from_currency} not found in the response"
            )
    else:
        raise Exception("Failed to get the conversion rate")


def fetch_prices_batch(symbols):
    """
    Fetch close prices using chunked yf.download() calls to avoid rate limiting.
    Splits symbols into chunks of BATCH_SIZE with INTER_BATCH_DELAY between each chunk.
    Each chunk is retried with exponential backoff on rate limit errors.

    @param symbols: List of stock symbols
    @return: Dict of {symbol: price}
    """
    if not symbols:
        return {}

    prices = {}
    chunks = [symbols[i:i + BATCH_SIZE] for i in range(0, len(symbols), BATCH_SIZE)]

    for idx, chunk in enumerate(chunks):
        if idx > 0:
            time.sleep(INTER_BATCH_DELAY)

        def _download_chunk():
            data = yf.download(chunk, period="5d", progress=False, auto_adjust=True)
            # yf.download doesn't raise on rate limit, just returns empty DataFrame
            # We need to raise manually to trigger retry
            if data.empty:
                raise Exception("rate limit... yfinance returned empty DataFrame")
            return data

        try:
            data = retry_call(_download_chunk, f"chunk {idx + 1}/{len(chunks)}")
        except Exception as e:
            print(
                f"[WARN] Chunk {idx + 1}/{len(chunks)} download failed after retries: {e}",
                file=sys.stderr,
            )
            continue

        if len(chunk) == 1:
            symbol = chunk[0]
            if "Close" in data.columns:
                series = data["Close"].dropna()
                if not series.empty:
                    prices[symbol] = float(series.iloc[-1])
        else:
            if "Close" in data.columns:
                close = data["Close"]
                for symbol in chunk:
                    try:
                        series = close[symbol].dropna()
                        if not series.empty:
                            prices[symbol] = float(series.iloc[-1])
                    except (KeyError, IndexError):
                        pass

    return prices


def fetch_symbol_info(symbol):
    """
    Fetch currency and website for a single symbol.
    Returns safe defaults on any failure — info is optional.

    @param symbol: Stock symbol
    @return: Tuple of (currency, website)
    """
    try:
        ticker = yf.Ticker(symbol, session=session)
        info = ticker.info
        return info.get("currency", "USD"), info.get("website")
    except Exception:
        return "USD", None


def main():
    if len(sys.argv) < 2:
        print("Usage: python get_price.py <symbol1> <symbol2> ... <symbolN>")
        sys.exit(1)

    symbols = sys.argv[1:]
    conversion_rates = {DEFAULT_CURRENCY: 1.0}
    result = {}

    # Fetch prices using chunked batch downloads
    try:
        prices = fetch_prices_batch(symbols)
    except Exception as e:
        print(f"[WARN] Price fetch failed: {e}", file=sys.stderr)
        prices = {}

    # Fetch info (currency + website) only for symbols that have prices
    symbols_with_prices = [s for s in symbols if s in prices]
    infos = {}
    if symbols_with_prices:
        with ThreadPoolExecutor(max_workers=MAX_INFO_WORKERS) as executor:
            info_futures = {executor.submit(fetch_symbol_info, s): s for s in symbols_with_prices}
            infos = {info_futures[f]: f.result() for f in as_completed(info_futures)}

    # Build result
    for symbol in symbols:
        price = prices.get(symbol)
        if price is None:
            result[symbol] = {"error": f"No price data available for {symbol}"}
            continue

        currency, website = infos.get(symbol, ("USD", None))

        if currency != DEFAULT_CURRENCY and currency not in conversion_rates:
            try:
                conversion_rates[currency] = get_conversion_rate(currency)
            except Exception:
                conversion_rates[currency] = 1.0

        result[symbol] = {"price": price * conversion_rates.get(currency, 1.0)}

        if website:
            result[symbol]["website"] = website

    print(json.dumps(result))


if __name__ == "__main__":
    main()
