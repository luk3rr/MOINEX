#!/usr/bin/env python

# Filename: get_stock_price.py
# Created on: January 12, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
from yahoofinancials import YahooFinancials
import requests

DEFAULT_CURRENCY = "BRL"


def get_conversion_rate(from_currency, to_currency=DEFAULT_CURRENCY):
    """
    Convert the amount from one currency to another using the exchange rate API
    """
    url = f"https://api.exchangerate-api.com/v4/latest/{from_currency}"
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


if len(sys.argv) < 2:
    print("Usage: python get_price.py <symbol1> <symbol2> ... <symbolN>")
    sys.exit(1)

symbols = sys.argv[1:]
result = {}

# Get the stock price
yahoo_financials = YahooFinancials(symbols)
prices = yahoo_financials.get_current_price()
currencies = yahoo_financials.get_currency()

conversion_rates = {DEFAULT_CURRENCY: 1}

for symbol in symbols:
    try:
        if symbol not in prices:
            raise ValueError(f"Symbol {symbol} not found")

        currency = currencies[symbol]
        price = prices[symbol]

        if currency != DEFAULT_CURRENCY:
            if currency not in conversion_rates:
                conversion_rates[currency] = get_conversion_rate(currency)

        result[symbol] = {"price": price * conversion_rates.get(currency, 1)}

    except Exception as e:
        result[symbol] = {"error": str(e)}

print(json.dumps(result))
