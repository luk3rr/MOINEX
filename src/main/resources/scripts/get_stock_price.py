#!/usr/bin/env python

# Filename: get_stock_price.py
# Created on: January 12, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
import yfinance as yf
import requests
import time
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


def get_ticker_data(symbol):
    """
    Get ticker data

    @param symbol: Stock symbol
    @return: Tuple of (price, currency, website)
    """
    # Create ticker with custom session
    ticker = yf.Ticker(symbol, session=session)

    # Get historical data with fallback
    hist = None
    try:
        hist = ticker.history(period="5d")
    except:
        hist = ticker.history(period="1mo")

    if hist is None or hist.empty:
        raise ValueError(f"No price data available for {symbol}")

    price = hist.iloc[-1]["Close"]

    # Get currency and website with error handling
    try:
        info = ticker.info
        currency = info.get("currency", "USD")
        website = info.get("website")
    except:
        # If info fails, use defaults
        currency = "USD"
        website = None

    return price, currency, website


def main():
    if len(sys.argv) < 2:
        print("Usage: python get_price.py <symbol1> <symbol2> ... <symbolN>")
        sys.exit(1)

    symbols = sys.argv[1:]
    conversion_rates = {DEFAULT_CURRENCY: 1.0}
    result = {}

    # Process symbols one by one
    for symbol in symbols:
        try:
            price, currency, website = get_ticker_data(symbol)

            # Get conversion rate if needed
            if currency != DEFAULT_CURRENCY:
                if currency not in conversion_rates:
                    conversion_rates[currency] = get_conversion_rate(currency)

            result[symbol] = {"price": price * conversion_rates.get(currency, 1.0)}

            if website:
                result[symbol]["website"] = website

        except Exception as e:
            result[symbol] = {"error": str(e)}

    print(json.dumps(result))


if __name__ == "__main__":
    main()
