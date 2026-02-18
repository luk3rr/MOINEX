#!/usr/bin/env python

# Filename: get_stock_price_history.py
# Created on: February 17, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
import yfinance as yf
import requests
import time
from datetime import datetime, timedelta
from urllib.parse import urlparse

DEFAULT_CURRENCY = "BRL"
EXCHANGE_API_URL_BASE = "https://api.exchangerate-api.com/v4/latest"


def get_conversion_rate(from_currency: str, to_currency: str = DEFAULT_CURRENCY) -> float:
    """
    Convert the amount from one currency to another using the exchange rate API

    @param from_currency: The currency to convert from
    @param to_currency: The currency to convert to
    @return: The conversion rate from the source currency to the target currency
    """
    if from_currency == "USX":
        base_rate = get_conversion_rate("USD", to_currency)
        return base_rate / 100
    elif to_currency == "USX":
        base_rate = get_conversion_rate(from_currency, "USD")
        return base_rate * 100

    url = f"{EXCHANGE_API_URL_BASE}/{from_currency}"
    response = requests.get(url)

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


def get_month_end_date(year: int, month: int) -> datetime:
    """
    Get the last day of a given month
    
    @param year: Year
    @param month: Month (1-12)
    @return: Last day of the month
    """
    if month == 12:
        return datetime(year, month, 31)
    else:
        next_month = datetime(year, month + 1, 1)
        return next_month - timedelta(days=1)


def main():
    if len(sys.argv) < 3:
        print("Usage: python get_stock_price_history.py <symbol> <start_date> [end_date] [specific_dates_json]")
        print("Dates format: YYYY-MM-DD")
        print("specific_dates_json: Optional JSON array of specific dates to fetch")
        sys.exit(1)

    symbol = sys.argv[1]
    start_date = sys.argv[2]
    end_date = sys.argv[3] if len(sys.argv) > 3 else datetime.now().strftime("%Y-%m-%d")
    
    # Parse specific dates if provided
    specific_dates = []
    if len(sys.argv) > 4:
        try:
            specific_dates = json.loads(sys.argv[4])
        except:
            specific_dates = []

    try:
        ticker = yf.Ticker(symbol)
        
        # Parse end_date to check if it's today or recent
        end_date_obj = datetime.strptime(end_date, "%Y-%m-%d")
        today = datetime.now()
        days_diff = (today.date() - end_date_obj.date()).days
        
        # Get historical data with fallback strategy
        hist = None
        
        # First try: exact dates
        try:
            hist = ticker.history(start=start_date, end=end_date)
            if hist is not None and not hist.empty:
                pass  # Success, continue
            else:
                hist = None  # Mark as failed to trigger fallback
        except Exception as e:
            hist = None  # Mark as failed to trigger fallback
        
        # Fallback: if end_date is today or up to 5 days in the past, fetch last 15 days
        if hist is None or hist.empty:
            if days_diff >= 0 and days_diff <= 5:
                try:
                    # Don't pass start_date, just use period to get recent data
                    hist = ticker.history(period="15d")
                    if hist is not None and not hist.empty:
                        # Filter to only include data from start_date onwards
                        start_date_obj = datetime.strptime(start_date, "%Y-%m-%d")
                        hist = hist[hist.index.date >= start_date_obj.date()]
                        if hist is not None and not hist.empty:
                            pass  # Success
                        else:
                            hist = None
                    else:
                        hist = None
                except Exception as e:
                    hist = None
        
        # Second fallback: try with 365 days
        if hist is None or hist.empty:
            try:
                hist = ticker.history(period="365d")
                if hist is not None and not hist.empty:
                    pass  # Success
                else:
                    hist = None
            except Exception as e:
                hist = None
        
        # Third fallback: try with max period
        if hist is None or hist.empty:
            try:
                hist = ticker.history(period="max")
                if hist is not None and not hist.empty:
                    pass  # Success
                else:
                    hist = None
            except Exception as e:
                hist = None
        
        # If still no data, return error
        if hist is None or hist.empty:
            error_msg = f"No data found for {symbol} in the date range {start_date} to {end_date}"
            print(json.dumps({"error": error_msg}))
            sys.exit(1)

        currency = ticker.info.get("currency", DEFAULT_CURRENCY)
        
        # Get conversion rate if needed
        conversion_rate = 1.0
        if currency != DEFAULT_CURRENCY:
            conversion_rate = get_conversion_rate(currency)

        # Parse start and end dates
        start_date_obj = datetime.strptime(start_date, "%Y-%m-%d")
        end_date_obj = datetime.strptime(end_date, "%Y-%m-%d")
        
        # Filter history to requested date range
        hist = hist[(hist.index.date >= start_date_obj.date()) & (hist.index.date <= end_date_obj.date())]
        
        if hist.empty:
            error_msg = f"No data found for {symbol} in the date range {start_date} to {end_date}"
            print(json.dumps({"error": error_msg}))
            sys.exit(1)
        
        result = {
            "symbol": symbol,
            "currency": currency,
            "conversion_rate": conversion_rate,
            "prices": []
        }

        # Group by month to identify month-end dates
        hist['YearMonth'] = hist.index.to_period('M')
        month_end_dates = set()
        
        for period, group in hist.groupby('YearMonth'):
            last_date = group.iloc[-1].name.date()
            month_end_dates.add(last_date)
        
        # Convert specific dates to date objects
        specific_dates_set = set()
        for date_str in specific_dates:
            try:
                specific_dates_set.add(datetime.strptime(date_str, "%Y-%m-%d").date())
            except:
                pass
        
        # Store only specific dates + month-end dates
        for index, row in hist.iterrows():
            actual_date = index.date()
            is_month_end = actual_date in month_end_dates
            is_specific_date = actual_date in specific_dates_set
            
            # Only store if it's a month-end OR a specific requested date
            if is_month_end or is_specific_date or not specific_dates:
                price_date = actual_date.strftime("%Y-%m-%d")
                closing_price = float(row['Close']) * conversion_rate
                
                result["prices"].append({
                    "date": price_date,
                    "price": closing_price,
                    "is_month_end": is_month_end
                })

        print(json.dumps(result))

    except Exception as e:
        import traceback
        error_msg = str(e)
        tb_str = traceback.format_exc()
        print(json.dumps({"error": error_msg, "traceback": tb_str}))
        sys.exit(1)


if __name__ == "__main__":
    main()
