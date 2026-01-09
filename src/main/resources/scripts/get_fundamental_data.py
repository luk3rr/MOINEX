#!/usr/bin/env python3

# Filename: get_fundamental_data.py
# Created on: January 08, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
import argparse
import csv
import yfinance as yf
from datetime import datetime
from pathlib import Path

DEFAULT_CURRENCY = "BRL"


def create_percent_object(value, data_temporality="calculated", reference_date=None):
    """
    Create a percent object with value, type, data temporality and optional reference date
    
    :param value: Numeric value (as percentage)
    :param data_temporality: Type of data - "real_time", "historical", or "calculated"
    :param reference_date: Reference date for the data (ISO format string or None)
    :return: Object with value, type, data_temporality and reference_date or None
    """
    if value is None:
        return None
    obj = {
        "value": value,
        "type": "percent",
        "data_temporality": data_temporality
    }
    if reference_date:
        obj["reference_date"] = reference_date
    return obj


def create_currency_object(value, data_temporality="calculated", reference_date=None):
    """
    Create a currency object with value, type, data temporality and optional reference date
    Currency code is available at root level, no need to duplicate
    
    :param value: Numeric value
    :param data_temporality: Type of data - "real_time", "historical", or "calculated"
    :param reference_date: Reference date for the data (ISO format string or None)
    :return: Object with value, type, data_temporality and reference_date or None
    """
    if value is None:
        return None
    obj = {
        "value": value,
        "type": "currency",
        "data_temporality": data_temporality
    }
    if reference_date:
        obj["reference_date"] = reference_date
    return obj


def create_ratio_object(value, data_temporality="calculated", reference_date=None):
    """
    Create a ratio object with value, type, data temporality and optional reference date
    
    :param value: Numeric value (ratio/multiple)
    :param data_temporality: Type of data - "real_time", "historical", or "calculated"
    :param reference_date: Reference date for the data (ISO format string or None)
    :return: Object with value, type, data_temporality and reference_date or None
    """
    if value is None:
        return None
    obj = {
        "value": value,
        "type": "ratio",
        "data_temporality": data_temporality
    }
    if reference_date:
        obj["reference_date"] = reference_date
    return obj


def create_number_object(value, data_temporality="calculated", reference_date=None):
    """
    Create a number object with value, type, data temporality and optional reference date
    
    :param value: Numeric value
    :param data_temporality: Type of data - "real_time", "historical", or "calculated"
    :param reference_date: Reference date for the data (ISO format string or None)
    :return: Object with value, type, data_temporality and reference_date or None
    """
    if value is None:
        return None
    obj = {
        "value": value,
        "type": "number",
        "data_temporality": data_temporality
    }
    if reference_date:
        obj["reference_date"] = reference_date
    return obj


def safe_get(data, key, default=None):
    """
    Safely get a value from a dictionary
    
    :param data: Dictionary to get value from
    :param key: Key to get
    :param default: Default value if key not found
    :return: Value or default
    """
    try:
        value = data.get(key, default)
        if value is None or (isinstance(value, float) and (value != value)):  # Check for NaN
            return default
        return value
    except:
        return default


def extract_value(obj):
    """
    Extract value from complex object or return as is
    
    :param obj: Object (can be dict with 'value' key or simple value)
    :return: Extracted value or empty string
    """
    if obj is None:
        return ""
    if isinstance(obj, dict) and "value" in obj:
        return obj["value"] if obj["value"] is not None else ""
    return obj


def get_unit_from_type(obj_type):
    """
    Get unit string from object type
    
    :param obj_type: Type string (percent, currency, ratio, number)
    :return: Unit string
    """
    if obj_type == "percent":
        return "%"
    elif obj_type == "ratio":
        return "x"
    elif obj_type == "currency":
        return ""  # Currency is at root level
    elif obj_type == "number":
        return ""
    return ""


def calculate_roe(net_income, shareholders_equity):
    """
    Calculate ROE (Return on Equity)
    
    :param net_income: Net income
    :param shareholders_equity: Shareholders equity
    :return: ROE as percentage or None
    """
    if shareholders_equity and shareholders_equity != 0:
        return (net_income / shareholders_equity) * 100
    return None


def calculate_roic(ebit, tax_rate, total_debt, shareholders_equity, cash):
    """
    Calculate ROIC (Return on Invested Capital)
    ROIC = NOPAT / Invested Capital
    NOPAT = EBIT * (1 - Tax Rate)
    Invested Capital = Total Debt + Shareholders Equity - Cash
    
    :param ebit: EBIT (Earnings Before Interest and Taxes)
    :param tax_rate: Tax rate
    :param total_debt: Total debt
    :param shareholders_equity: Shareholders equity
    :param cash: Cash and cash equivalents
    :return: ROIC as percentage or None
    """
    try:
        nopat = ebit * (1 - tax_rate)
        invested_capital = total_debt + shareholders_equity - cash
        
        if invested_capital and invested_capital != 0:
            return (nopat / invested_capital) * 100
    except:
        pass
    return None


def calculate_net_margin(net_income, revenue):
    """
    Calculate Net Margin
    
    :param net_income: Net income
    :param revenue: Total revenue
    :return: Net margin as percentage or None
    """
    if revenue and revenue != 0:
        return (net_income / revenue) * 100
    return None


def calculate_ebitda_margin(ebitda, revenue):
    """
    Calculate EBITDA Margin
    
    :param ebitda: EBITDA
    :param revenue: Total revenue
    :return: EBITDA margin as percentage or None
    """
    if revenue and revenue != 0:
        return (ebitda / revenue) * 100
    return None


def calculate_net_debt(total_debt, cash):
    """
    Calculate Net Debt
    
    :param total_debt: Total debt
    :param cash: Cash and cash equivalents
    :return: Net debt or None
    """
    try:
        return total_debt - cash
    except:
        return None


def calculate_net_debt_to_ebitda(net_debt, ebitda):
    """
    Calculate Net Debt / EBITDA ratio
    
    :param net_debt: Net debt
    :param ebitda: EBITDA
    :return: Ratio or None
    """
    if ebitda and ebitda != 0:
        return net_debt / ebitda
    return None


def calculate_current_ratio(current_assets, current_liabilities):
    """
    Calculate Current Ratio (Liquidez Corrente)
    
    :param current_assets: Current assets
    :param current_liabilities: Current liabilities
    :return: Current ratio or None
    """
    if current_liabilities and current_liabilities != 0:
        return current_assets / current_liabilities
    return None


def calculate_pe_ratio(price, earnings_per_share):
    """
    Calculate P/E ratio
    
    :param price: Current stock price
    :param earnings_per_share: Earnings per share
    :return: P/E ratio or None
    """
    if earnings_per_share and earnings_per_share != 0:
        return price / earnings_per_share
    return None


def calculate_earnings_yield(earnings_per_share, price):
    """
    Calculate Earnings Yield
    
    :param earnings_per_share: Earnings per share
    :param price: Current stock price
    :return: Earnings yield as percentage or None
    """
    if price and price != 0:
        return (earnings_per_share / price) * 100
    return None


def calculate_fcf_yield(free_cash_flow, market_cap):
    """
    Calculate FCF Yield
    
    :param free_cash_flow: Free cash flow
    :param market_cap: Market capitalization
    :return: FCF yield as percentage or None
    """
    if market_cap and market_cap != 0:
        return (free_cash_flow / market_cap) * 100
    return None


def calculate_dividend_yield(dividend_rate, current_price):
    """
    Calculate Dividend Yield
    
    :param dividend_rate: Annual dividend per share
    :param current_price: Current stock price
    :return: Dividend yield as percentage or None
    """
    if current_price and current_price != 0:
        return (dividend_rate / current_price) * 100
    return None


def calculate_payout_ratio(dividends_paid, net_income):
    """
    Calculate Payout Ratio
    
    :param dividends_paid: Total dividends paid
    :param net_income: Net income
    :return: Payout ratio as percentage or None
    """
    if net_income and net_income != 0:
        return (abs(dividends_paid) / net_income) * 100
    return None


def calculate_graham_number(eps, book_value_per_share):
    """
    Calculate Graham Number (Benjamin Graham's intrinsic value - simplified formula)
    
    Formula: √(22.5 × EPS × Book Value per Share)
    
    :param eps: Earnings per share
    :param book_value_per_share: Book value per share
    :return: Graham number or None
    """
    try:
        if eps and book_value_per_share and eps > 0 and book_value_per_share > 0:
            import math
            return math.sqrt(22.5 * eps * book_value_per_share)
    except:
        pass
    return None


def calculate_graham_fair_value(eps, growth_rate, bond_yield=0.045):
    """
    Calculate Fair Value using Benjamin Graham's revised formula
    
    Formula: (EPS × (8.5 + 2g) × 4.4) / Y
    
    Where:
    - EPS = Earnings per share
    - g = Expected growth rate (as decimal, e.g., 0.10 for 10%)
    - 8.5 = P/E base for no-growth company
    - 4.4 = Average yield of AAA corporate bonds in 1962
    - Y = Current yield of AAA corporate bonds (default 4.5%)
    
    :param eps: Earnings per share
    :param growth_rate: Expected annual growth rate (as decimal)
    :param bond_yield: Current AAA corporate bond yield (default 4.5%)
    :return: Fair value or None
    """
    try:
        if eps and eps > 0 and growth_rate is not None:
            # Cap growth rate at 25% as per Graham's recommendation
            g = min(growth_rate, 0.25)
            fair_value = (eps * (8.5 + 2 * g * 100) * 4.4) / (bond_yield * 100)
            return fair_value
    except:
        pass
    return None


def calculate_price_change(current_price, previous_price):
    """
    Calculate price change percentage
    
    :param current_price: Current price
    :param previous_price: Previous price
    :return: Price change as percentage or None
    """
    try:
        if current_price and previous_price and previous_price != 0:
            return ((current_price - previous_price) / previous_price) * 100
    except:
        pass
    return None


def get_price_performance(ticker):
    """
    Get price performance metrics for different periods
    
    :param ticker: yfinance Ticker object
    :return: Dictionary with price performance metrics
    """
    try:
        from datetime import datetime, timedelta
        import pandas as pd
        
        # Get historical data (1 year + buffer)
        hist = ticker.history(period="1y")
        
        if hist.empty:
            return None
        
        current_price = hist['Close'].iloc[-1]
        
        performance = {
            "current_price": create_currency_object(current_price, "real_time"),
            "day_high": create_currency_object(hist['High'].iloc[-1], "real_time"),
            "day_low": create_currency_object(hist['Low'].iloc[-1], "real_time"),
        }
        
        # 1 day change (if we have at least 2 days)
        if len(hist) >= 2:
            prev_close = hist['Close'].iloc[-2]
            performance["change_1d"] = create_percent_object(calculate_price_change(current_price, prev_close), "historical")
        
        # 5 days (1 week) change
        if len(hist) >= 6:
            price_5d_ago = hist['Close'].iloc[-6]
            performance["change_5d"] = create_percent_object(calculate_price_change(current_price, price_5d_ago), "historical")
        
        # 1 month change (approximately 21 trading days)
        if len(hist) >= 22:
            price_1m_ago = hist['Close'].iloc[-22]
            performance["change_1m"] = create_percent_object(calculate_price_change(current_price, price_1m_ago), "historical")
        
        # 3 months change (approximately 63 trading days)
        if len(hist) >= 64:
            price_3m_ago = hist['Close'].iloc[-64]
            performance["change_3m"] = create_percent_object(calculate_price_change(current_price, price_3m_ago), "historical")
        
        # 6 months change (approximately 126 trading days)
        if len(hist) >= 127:
            price_6m_ago = hist['Close'].iloc[-127]
            performance["change_6m"] = create_percent_object(calculate_price_change(current_price, price_6m_ago), "historical")
        
        # 52 weeks (1 year) change
        if len(hist) >= 2:
            price_52w_ago = hist['Close'].iloc[0]
            performance["change_52w"] = create_percent_object(calculate_price_change(current_price, price_52w_ago), "historical")
        
        # Year to Date (YTD) change
        current_year = datetime.now().year
        ytd_data = hist[hist.index.year == current_year]
        if not ytd_data.empty and len(ytd_data) > 1:
            price_ytd_start = ytd_data['Close'].iloc[0]
            performance["change_ytd"] = create_percent_object(calculate_price_change(current_price, price_ytd_start), "historical")
        
        # 52-week high and low
        week_52_high = hist['High'].max()
        week_52_low = hist['Low'].min()
        performance["week_52_high"] = create_currency_object(week_52_high, "historical")
        performance["week_52_low"] = create_currency_object(week_52_low, "historical")
        
        # Distance from 52-week high/low
        performance["distance_from_52w_high"] = create_percent_object(calculate_price_change(current_price, week_52_high), "calculated")
        performance["distance_from_52w_low"] = create_percent_object(calculate_price_change(current_price, week_52_low), "calculated")
        
        return performance
        
    except Exception as e:
        return None


def calculate_revenue_growth(revenues, reference_date=None):
    """
    Calculate revenue growth (YoY and CAGR)
    
    :param revenues: List of revenues (most recent first)
    :param reference_date: Reference date for the most recent data
    :return: Dictionary with growth metrics or None
    """
    try:
        if len(revenues) < 2:
            return None
        
        # YoY growth (most recent year)
        yoy_growth = ((revenues[0] - revenues[1]) / revenues[1]) * 100 if revenues[1] != 0 else None
        
        # CAGR (if we have enough data)
        cagr = None
        if len(revenues) >= 3:
            n_years = len(revenues) - 1
            if revenues[-1] != 0:
                cagr = (((revenues[0] / revenues[-1]) ** (1 / n_years)) - 1) * 100
        
        return {
            "yoy_growth": create_percent_object(yoy_growth, "calculated", reference_date),
            "cagr": create_percent_object(cagr, "calculated", reference_date),
            "years": len(revenues)
        }
    except:
        return None


def get_fundamental_data(symbol: str, period: str = "annual") -> dict:
    """
    Get fundamental data for a stock symbol
    
    :param symbol: Stock symbol (e.g., PETR4.SA for Petrobras)
    :param period: Period type - 'annual' or 'quarterly'
    :return: Dictionary with fundamental data
    """
    try:
        ticker = yf.Ticker(symbol)
        info = ticker.info
        
        # Get financial statements based on period
        if period == "quarterly":
            financials = ticker.quarterly_financials
            balance_sheet = ticker.quarterly_balance_sheet
            cashflow = ticker.quarterly_cashflow
        else:
            financials = ticker.financials
            balance_sheet = ticker.balance_sheet
            cashflow = ticker.cashflow
        
        result = {
            "symbol": symbol,
            "company_name": safe_get(info, "longName", symbol),
            "sector": safe_get(info, "sector"),
            "industry": safe_get(info, "industry"),
            "currency": safe_get(info, "currency", DEFAULT_CURRENCY),
            "period_type": period,
            "last_updated": datetime.now().isoformat(),
        }
        
        # Current price data
        current_price = safe_get(info, "currentPrice")
        market_cap = safe_get(info, "marketCap")
        
        # Get price performance metrics
        price_performance = get_price_performance(ticker)
        if price_performance:
            result["price_performance"] = price_performance
        
        # Get most recent financial data
        if not financials.empty and not balance_sheet.empty:
            latest_financials = financials.iloc[:, 0]
            latest_balance = balance_sheet.iloc[:, 0]
            
            # Get reference dates for transparency
            financials_date = financials.columns[0].strftime("%Y-%m-%d") if not financials.empty else None
            balance_date = balance_sheet.columns[0].strftime("%Y-%m-%d") if not balance_sheet.empty else None
            cashflow_date = cashflow.columns[0].strftime("%Y-%m-%d") if not cashflow.empty else None
            
            result["data_reference"] = {
                "financials_date": financials_date,
                "balance_sheet_date": balance_date,
                "cashflow_date": cashflow_date,
                "period_type": period,
            }
            
            # Income statement data
            revenue = safe_get(latest_financials, "Total Revenue", 0)
            net_income = safe_get(latest_financials, "Net Income", 0)
            ebit = safe_get(latest_financials, "EBIT", 0)
            ebitda = safe_get(latest_financials, "EBITDA", 0)
            
            # Balance sheet data
            total_assets = safe_get(latest_balance, "Total Assets", 0)
            current_assets = safe_get(latest_balance, "Current Assets", 0)
            current_liabilities = safe_get(latest_balance, "Current Liabilities", 0)
            total_debt = safe_get(latest_balance, "Total Debt", 0)
            shareholders_equity = safe_get(latest_balance, "Stockholders Equity", 0)
            cash = safe_get(latest_balance, "Cash And Cash Equivalents", 0)
            
            # Calculate metrics
            
            # 1. Rentabilidade (Profitability)
            roe = calculate_roe(net_income, shareholders_equity)
            net_margin = calculate_net_margin(net_income, revenue)
            ebitda_margin = calculate_ebitda_margin(ebitda, revenue)
            
            # Tax rate estimation
            tax_rate = 0.34  # Default Brazilian corporate tax rate
            income_tax = safe_get(latest_financials, "Tax Provision")
            if income_tax and ebit and ebit != 0:
                tax_rate = abs(income_tax / ebit)
            
            roic = calculate_roic(ebit, tax_rate, total_debt, shareholders_equity, cash)
            
            result["profitability"] = {
                "roe": create_percent_object(roe, "calculated", financials_date),
                "roic": create_percent_object(roic, "calculated", balance_date),
                "net_margin": create_percent_object(net_margin, "calculated", financials_date),
                "ebitda_margin": create_percent_object(ebitda_margin, "calculated", financials_date),
            }
            
            # 2. Crescimento (Growth)
            revenue_growth = None
            if not financials.empty and len(financials.columns) > 1:
                revenues = [safe_get(financials.iloc[:, i], "Total Revenue", 0) 
                           for i in range(min(5, len(financials.columns)))]
                revenue_growth = calculate_revenue_growth(revenues, financials_date)
            
            result["growth"] = {
                "revenue_growth": revenue_growth,
            }
            
            # 3. Endividamento (Debt)
            net_debt = calculate_net_debt(total_debt, cash)
            net_debt_to_ebitda = calculate_net_debt_to_ebitda(net_debt, ebitda)
            current_ratio = calculate_current_ratio(current_assets, current_liabilities)
            
            result["debt"] = {
                "total_debt": create_currency_object(total_debt, "calculated", balance_date),
                "net_debt": create_currency_object(net_debt, "calculated", balance_date),
                "net_debt_to_ebitda": create_ratio_object(net_debt_to_ebitda, "calculated", balance_date),
                "current_ratio": create_ratio_object(current_ratio, "calculated", balance_date),
            }
            
            # 4. Eficiência Operacional (Operational Efficiency)
            asset_turnover = revenue / total_assets if total_assets != 0 else None
            
            result["efficiency"] = {
                "asset_turnover": create_ratio_object(asset_turnover, "calculated", balance_date),
                "ebitda": create_currency_object(ebitda, "calculated", financials_date),
            }
            
            # 5. Geração de Caixa (Cash Generation)
            free_cash_flow = None
            if not cashflow.empty:
                latest_cashflow = cashflow.iloc[:, 0]
                operating_cash_flow = safe_get(latest_cashflow, "Operating Cash Flow", 0)
                capex = safe_get(latest_cashflow, "Capital Expenditure", 0)
                free_cash_flow = operating_cash_flow + capex  # capex is usually negative
                
                fcf_to_net_income = free_cash_flow / net_income if net_income != 0 else None
                
                result["cash_generation"] = {
                    "free_cash_flow": create_currency_object(free_cash_flow, "calculated", cashflow_date),
                    "operating_cash_flow": create_currency_object(operating_cash_flow, "calculated", cashflow_date),
                    "capex": create_currency_object(capex, "calculated", cashflow_date),
                    "fcf_to_net_income": create_ratio_object(fcf_to_net_income, "calculated", cashflow_date),
                }
            
            # 6. Valuation
            eps = safe_get(info, "trailingEps")
            pe_ratio = calculate_pe_ratio(current_price, eps) if current_price and eps else None
            earnings_yield = calculate_earnings_yield(eps, current_price) if current_price and eps else None
            fcf_yield = calculate_fcf_yield(free_cash_flow, market_cap) if free_cash_flow and market_cap else None
            
            # EV/EBITDA
            enterprise_value = safe_get(info, "enterpriseValue")
            ev_to_ebitda = enterprise_value / ebitda if enterprise_value and ebitda and ebitda != 0 else None
            
            # Dividend metrics
            dividend_rate = safe_get(info, "dividendRate")  # Annual dividend per share
            dividend_yield = calculate_dividend_yield(dividend_rate, current_price) if dividend_rate and current_price else None
            
            # Payout ratio
            payout_ratio = None
            if not cashflow.empty:
                latest_cashflow = cashflow.iloc[:, 0]
                dividends_paid = safe_get(latest_cashflow, "Cash Dividends Paid", 0)
                if dividends_paid and net_income:
                    payout_ratio = calculate_payout_ratio(dividends_paid, net_income)
            
            # Benjamin Graham's Fair Value calculations
            book_value_per_share = safe_get(info, "bookValue")
            graham_number = calculate_graham_number(eps, book_value_per_share) if eps and book_value_per_share else None
            
            # For Graham revised formula, use revenue CAGR as growth estimate
            growth_estimate = None
            graham_fair_value = None
            if revenue_growth and revenue_growth.get("cagr") and revenue_growth["cagr"]:
                cagr_value = revenue_growth["cagr"]["value"] if isinstance(revenue_growth["cagr"], dict) else revenue_growth["cagr"]
                growth_estimate = cagr_value / 100  # Convert to decimal
                graham_fair_value = calculate_graham_fair_value(eps, growth_estimate)
            
            # Calculate margin of safety
            margin_of_safety = None
            fair_value_used = graham_fair_value if graham_fair_value else graham_number
            if fair_value_used and current_price:
                margin_of_safety = ((fair_value_used - current_price) / fair_value_used) * 100
            
            result["valuation"] = {
                "current_price": create_currency_object(current_price, "real_time"),
                "market_cap": create_currency_object(market_cap, "real_time"),
                "enterprise_value": create_currency_object(enterprise_value, "calculated", balance_date),
                "pe_ratio": create_ratio_object(pe_ratio, "calculated", financials_date),
                "ev_to_ebitda": create_ratio_object(ev_to_ebitda, "calculated", financials_date),
                "earnings_yield": create_percent_object(earnings_yield, "calculated", financials_date),
                "fcf_yield": create_percent_object(fcf_yield, "calculated", cashflow_date),
                "dividend_yield": create_percent_object(dividend_yield, "calculated", financials_date),
                "dividend_rate": create_currency_object(dividend_rate, "historical", financials_date),
                "payout_ratio": create_percent_object(payout_ratio, "calculated", cashflow_date),
                "graham_number": create_currency_object(graham_number, "calculated", financials_date),
                "graham_fair_value": create_currency_object(graham_fair_value, "calculated", financials_date),
                "margin_of_safety": create_percent_object(margin_of_safety, "calculated", financials_date),
            }
            
        else:
            result["error"] = "Financial data not available"
        
        return result
        
    except Exception as e:
        return {
            "symbol": symbol,
            "error": str(e)
        }


def export_to_csv(symbol: str, data: dict, output_dir: str = ".", period: str = "annual"):
    """
    Export fundamental data to CSV files (raw data + calculated metrics)
    
    :param symbol: Stock symbol
    :param data: Fundamental data dictionary
    :param output_dir: Output directory for CSV files
    :param period: Period type - 'annual' or 'quarterly'
    """
    if "error" in data:
        print(f"Error for {symbol}: {data['error']}")
        return
    
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    
    # Get raw data from yfinance based on period
    ticker = yf.Ticker(symbol)
    if period == "quarterly":
        financials = ticker.quarterly_financials
        balance_sheet = ticker.quarterly_balance_sheet
        cashflow = ticker.quarterly_cashflow
    else:
        financials = ticker.financials
        balance_sheet = ticker.balance_sheet
        cashflow = ticker.cashflow
    
    # Export raw data to CSV
    if not financials.empty:
        financials_file = output_path / f"{symbol.replace('.', '_')}_financials.csv"
        financials.to_csv(financials_file)
        print(f"✓ Saved: {financials_file}")
    
    if not balance_sheet.empty:
        balance_file = output_path / f"{symbol.replace('.', '_')}_balance_sheet.csv"
        balance_sheet.to_csv(balance_file)
        print(f"✓ Saved: {balance_file}")
    
    if not cashflow.empty:
        cashflow_file = output_path / f"{symbol.replace('.', '_')}_cashflow.csv"
        cashflow.to_csv(cashflow_file)
        print(f"✓ Saved: {cashflow_file}")
    
    # Export calculated metrics to CSV
    metrics_file = output_path / f"{symbol.replace('.', '_')}_metrics.csv"
    
    with open(metrics_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(["Category", "Metric", "Value", "Unit"])
        
        # Basic info
        writer.writerow(["Info", "Symbol", data.get("symbol", ""), ""])
        writer.writerow(["Info", "Company Name", data.get("company_name", ""), ""])
        writer.writerow(["Info", "Sector", data.get("sector", ""), ""])
        writer.writerow(["Info", "Industry", data.get("industry", ""), ""])
        writer.writerow(["Info", "Currency", data.get("currency", ""), ""])
        writer.writerow(["Info", "Period Type", data.get("period_type", ""), ""])
        writer.writerow(["Info", "Last Updated", data.get("last_updated", ""), ""])
        
        # Data reference dates
        if "data_reference" in data:
            ref = data["data_reference"]
            writer.writerow(["Data Reference", "Financials Date", ref.get("financials_date", ""), ""])
            writer.writerow(["Data Reference", "Balance Sheet Date", ref.get("balance_sheet_date", ""), ""])
            writer.writerow(["Data Reference", "Cashflow Date", ref.get("cashflow_date", ""), ""])
        
        # Price Performance
        if "price_performance" in data:
            perf = data["price_performance"]
            writer.writerow(["Price Performance", "Current Price", extract_value(perf.get("current_price")), data.get("currency", "BRL")])
            writer.writerow(["Price Performance", "Day High", extract_value(perf.get("day_high")), data.get("currency", "BRL")])
            writer.writerow(["Price Performance", "Day Low", extract_value(perf.get("day_low")), data.get("currency", "BRL")])
            writer.writerow(["Price Performance", "Change 1 Day", extract_value(perf.get("change_1d")), "%"])
            writer.writerow(["Price Performance", "Change 5 Days", extract_value(perf.get("change_5d")), "%"])
            writer.writerow(["Price Performance", "Change 1 Month", extract_value(perf.get("change_1m")), "%"])
            writer.writerow(["Price Performance", "Change 3 Months", extract_value(perf.get("change_3m")), "%"])
            writer.writerow(["Price Performance", "Change 6 Months", extract_value(perf.get("change_6m")), "%"])
            writer.writerow(["Price Performance", "Change 52 Weeks", extract_value(perf.get("change_52w")), "%"])
            writer.writerow(["Price Performance", "Change YTD", extract_value(perf.get("change_ytd")), "%"])
            writer.writerow(["Price Performance", "52 Week High", extract_value(perf.get("week_52_high")), data.get("currency", "BRL")])
            writer.writerow(["Price Performance", "52 Week Low", extract_value(perf.get("week_52_low")), data.get("currency", "BRL")])
            writer.writerow(["Price Performance", "Distance from 52W High", extract_value(perf.get("distance_from_52w_high")), "%"])
            writer.writerow(["Price Performance", "Distance from 52W Low", extract_value(perf.get("distance_from_52w_low")), "%"])
        
        # Profitability
        if "profitability" in data:
            prof = data["profitability"]
            writer.writerow(["Profitability", "ROE", extract_value(prof.get("roe")), "%"])
            writer.writerow(["Profitability", "ROIC", extract_value(prof.get("roic")), "%"])
            writer.writerow(["Profitability", "Net Margin", extract_value(prof.get("net_margin")), "%"])
            writer.writerow(["Profitability", "EBITDA Margin", extract_value(prof.get("ebitda_margin")), "%"])
        
        # Growth
        if "growth" in data and data["growth"].get("revenue_growth"):
            growth = data["growth"]["revenue_growth"]
            writer.writerow(["Growth", "Revenue YoY Growth", extract_value(growth.get("yoy_growth")), "%"])
            writer.writerow(["Growth", "Revenue CAGR", extract_value(growth.get("cagr")), "%"])
            writer.writerow(["Growth", "Years Analyzed", growth.get("years", ""), "years"])
        
        # Debt
        if "debt" in data:
            debt = data["debt"]
            writer.writerow(["Debt", "Total Debt", extract_value(debt.get("total_debt")), data.get("currency", "BRL")])
            writer.writerow(["Debt", "Net Debt", extract_value(debt.get("net_debt")), data.get("currency", "BRL")])
            writer.writerow(["Debt", "Net Debt to EBITDA", extract_value(debt.get("net_debt_to_ebitda")), "x"])
            writer.writerow(["Debt", "Current Ratio", extract_value(debt.get("current_ratio")), ""])
        
        # Efficiency
        if "efficiency" in data:
            eff = data["efficiency"]
            writer.writerow(["Efficiency", "Asset Turnover", extract_value(eff.get("asset_turnover")), ""])
            writer.writerow(["Efficiency", "EBITDA", extract_value(eff.get("ebitda")), data.get("currency", "BRL")])
        
        # Cash Generation
        if "cash_generation" in data:
            cash = data["cash_generation"]
            writer.writerow(["Cash Generation", "Free Cash Flow", extract_value(cash.get("free_cash_flow")), data.get("currency", "BRL")])
            writer.writerow(["Cash Generation", "Operating Cash Flow", extract_value(cash.get("operating_cash_flow")), data.get("currency", "BRL")])
            writer.writerow(["Cash Generation", "CAPEX", extract_value(cash.get("capex")), data.get("currency", "BRL")])
            writer.writerow(["Cash Generation", "FCF to Net Income", extract_value(cash.get("fcf_to_net_income")), ""])
        
        # Valuation
        if "valuation" in data:
            val = data["valuation"]
            writer.writerow(["Valuation", "Current Price", extract_value(val.get("current_price")), data.get("currency", "BRL")])
            writer.writerow(["Valuation", "Market Cap", extract_value(val.get("market_cap")), data.get("currency", "BRL")])
            writer.writerow(["Valuation", "Enterprise Value", extract_value(val.get("enterprise_value")), data.get("currency", "BRL")])
            writer.writerow(["Valuation", "P/E Ratio", extract_value(val.get("pe_ratio")), "x"])
            writer.writerow(["Valuation", "EV to EBITDA", extract_value(val.get("ev_to_ebitda")), "x"])
            writer.writerow(["Valuation", "Earnings Yield", extract_value(val.get("earnings_yield")), "%"])
            writer.writerow(["Valuation", "FCF Yield", extract_value(val.get("fcf_yield")), "%"])
            writer.writerow(["Valuation", "Dividend Yield", extract_value(val.get("dividend_yield")), "%"])
            writer.writerow(["Valuation", "Dividend Rate", extract_value(val.get("dividend_rate")), data.get("currency", "BRL") + "/share"])
            writer.writerow(["Valuation", "Payout Ratio", extract_value(val.get("payout_ratio")), "%"])
            writer.writerow(["Valuation", "Graham Number", extract_value(val.get("graham_number")), data.get("currency", "BRL")])
            writer.writerow(["Valuation", "Graham Fair Value", extract_value(val.get("graham_fair_value")), data.get("currency", "BRL")])
            writer.writerow(["Valuation", "Margin of Safety", extract_value(val.get("margin_of_safety")), "%"])
    
    print(f"✓ Saved: {metrics_file}")
    print(f"\nAll files saved to: {output_path.absolute()}")


def main():
    parser = argparse.ArgumentParser(
        description="Get fundamental data for stocks",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # JSON output with annual data (default)
  python get_fundamental_data.py PETR4.SA VALE3.SA
  
  # JSON output with quarterly data
  python get_fundamental_data.py --period quarterly PETR4.SA
  
  # CSV output with raw data and calculated metrics
  python get_fundamental_data.py --format csv PETR4.SA
  
  # CSV output with quarterly data
  python get_fundamental_data.py --format csv --period quarterly PETR4.SA
  
  # CSV output to specific directory
  python get_fundamental_data.py --format csv --output ./data PETR4.SA VALE3.SA
        """
    )
    
    parser.add_argument(
        "symbols",
        nargs="+",
        help="Stock symbols (e.g., PETR4.SA VALE3.SA ITUB4.SA)"
    )
    
    parser.add_argument(
        "--format",
        choices=["json", "csv"],
        default="json",
        help="Output format: json (default) or csv"
    )
    
    parser.add_argument(
        "--output",
        default=".",
        help="Output directory for CSV files (only used with --format csv)"
    )
    
    parser.add_argument(
        "--period",
        choices=["annual", "quarterly"],
        default="annual",
        help="Period type: annual (default) or quarterly"
    )
    
    args = parser.parse_args()
    
    if args.format == "json":
        results = {}
        for symbol in args.symbols:
            results[symbol] = get_fundamental_data(symbol, args.period)
        print(json.dumps(results, indent=2, ensure_ascii=False))
    
    elif args.format == "csv":
        for symbol in args.symbols:
            print(f"\n{'='*60}")
            print(f"Processing {symbol} ({args.period})...")
            print(f"{'='*60}")
            data = get_fundamental_data(symbol, args.period)
            export_to_csv(symbol, data, args.output, args.period)
        
        print(f"\n{'='*60}")
        print(f"✓ Processing complete for {len(args.symbols)} symbol(s)")
        print(f"{'='*60}")


if __name__ == "__main__":
    main()
