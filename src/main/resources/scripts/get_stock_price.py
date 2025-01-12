#!/usr/bin/env python

# Filename: get_stock_price.py
# Created on: January 12, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

from yahoo_fin import stock_info
import sys

if len(sys.argv) != 2:
    print("Usage: python get_price.py <symbol>")
    sys.exit(1)

symbol = sys.argv[1]

try:
    price = stock_info.get_live_price(symbol)
    print(price)  # print the price to java app to read
except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
