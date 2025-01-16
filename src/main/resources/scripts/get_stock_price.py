#!/usr/bin/env python

# Filename: get_stock_price.py
# Created on: January 12, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
import yfinance as yf
import requests

DEFAULT_CURRENCY = "BRL"

EXCHANGE_API_URL_BASE = "https://api.exchangerate-api.com/v4/latest"


def get_conversion_rate(
    from_currency: str, to_currency: str = DEFAULT_CURRENCY
) -> float:
    """
    Convert the amount from one currency to another using the exchange rate API

    @param from_currency: The currency to convert from
    @param to_currency: The currency to convert to
    @return: The conversion rate from the source currency to the target currency
    """
    url = f"{EXCHANGE_API_URL_BASE}/{from_currency}"
    response = requests.get(url)

    if response.status_code == 200:
        data = response.json()
        # Check if the currency is in the response
        if to_currency in data["rates"]:
            return data["rates"][to_currency]
        else:
            raise ValueError(f"Currency {to_currency} not found in the response")
    else:
        raise Exception("Failed to get the conversion rate")


def main():
    if len(sys.argv) < 2:
        print("Usage: python get_price.py <symbol1> <symbol2> ... <symbolN>")
        sys.exit(1)

    symbols = sys.argv[1:]

    # Get the stock price
    tickers = yf.Tickers(" ".join(symbols))
    prices = {
        symbol: ticker.history().iloc[-1]["Close"]
        for symbol, ticker in tickers.tickers.items()
    }
    currencies = {
        symbol: ticker.info["currency"] for symbol, ticker in tickers.tickers.items()
    }

    conversion_rates = {DEFAULT_CURRENCY: 1}

    result = {}

    for symbol in symbols:
        try:
            if symbol not in prices:
                raise ValueError(f"Symbol {symbol} not found")

            currency = currencies[symbol]
            price = prices[symbol]

            if currency != DEFAULT_CURRENCY:
                if currency not in conversion_rates:
                    conversion_rates[currency] = get_conversion_rate(currency)

            result[symbol] = {"price": price * conversion_rates[currency]}

        except Exception as e:
            result[symbol] = {"error": str(e)}

    print(json.dumps(result))


if __name__ == "__main__":
    main()
