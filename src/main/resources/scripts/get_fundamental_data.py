#!/usr/bin/env python3

# Filename: get_fundamental_data.py
# Created on: January 08, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import os
import sys
import json
import argparse
import csv
import re
import time
import yfinance as yf
from datetime import datetime
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import yf_resilience as res

res.use_explicit_dns_on_windows()

DEFAULT_CURRENCY = "BRL"

EPS_DIVERGENCE_THRESHOLD = 0.25
NON_MONETARY_ROW_MARKERS = ("shares", "rate", "ratio", "percent")

SCHEMA_VERSION = 2
ASSET_KIND_COMPANY = "company"
ASSET_KIND_FUND = "fund"
ASSET_KIND_ETF = "etf"
BRAZILIAN_FUND_SYMBOL = re.compile(r"^[A-Z]{4}11$")
DIVIDEND_MONTHS = 12

REASON_NEGATIVE_EPS = "NEGATIVE_EPS"
REASON_NEGATIVE_EQUITY = "NEGATIVE_EQUITY"
REASON_NEGATIVE_GROWTH = "NEGATIVE_GROWTH"
REASON_INSUFFICIENT_HISTORY = "INSUFFICIENT_HISTORY"
REASON_NOT_APPLICABLE = "NOT_APPLICABLE"
REASON_DATA_UNAVAILABLE = "DATA_UNAVAILABLE"

METRIC_SPECS = {
    "profitability": {"roe": "percent", "roic": "percent", "net_margin": "percent", "ebitda_margin": "percent"},
    "debt": {
        "total_debt": "currency",
        "net_debt": "currency",
        "net_debt_to_ebitda": "ratio",
        "current_ratio": "ratio",
    },
    "efficiency": {"asset_turnover": "ratio", "ebitda": "currency"},
    "cash_generation": {
        "free_cash_flow": "currency",
        "operating_cash_flow": "currency",
        "capex": "currency",
        "fcf_to_net_income": "ratio",
    },
    "valuation": {
        "current_price": "currency",
        "market_cap": "currency",
        "enterprise_value": "currency",
        "eps": "currency",
        "pe_ratio": "ratio",
        "peg_ratio": "ratio",
        "eps_cagr": "percent",
        "ev_to_ebitda": "ratio",
        "earnings_yield": "percent",
        "fcf_yield": "percent",
        "dividend_yield": "percent",
        "dividend_rate": "currency",
        "payout_ratio": "percent",
        "graham_number": "currency",
        "graham_fair_value": "currency",
        "margin_of_safety": "percent",
    },
}

FUND_METRIC_SPECS = {
    "income": {"dividend_yield": "percent", "last_dividend": "currency", "dividends_12m_total": "currency"},
    "valuation": {"current_price": "currency", "market_cap": "currency", "price_to_book": "ratio"},
}


retry_call = res.retry_call


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

def calculate_eps_cagr(eps_values):
    """
    Calculate CAGR of EPS

    :param eps_values: List of EPS values (most recent first)
    :return: CAGR percentage or None
    """
    try:
        if len(eps_values) < 2:
            return None

        if eps_values[-1] <= 0:
            return None

        n_years = len(eps_values) - 1
        return (((eps_values[0] / eps_values[-1]) ** (1 / n_years)) - 1) * 100
    except Exception:
        return None

def calculate_roe(net_income, shareholders_equity):
    """
    Calculate ROE (Return on Equity)

    :param net_income: Net income
    :param shareholders_equity: Shareholders equity
    :return: ROE as percentage, or None when equity is zero or negative (the ratio would be misleading)
    """
    if shareholders_equity and shareholders_equity > 0:
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
    :return: P/E ratio, or None when earnings are zero or negative (a negative P/E means nothing)
    """
    if earnings_per_share and earnings_per_share > 0:
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


def calculate_graham_fair_value(eps, growth_rate, bond_yield=0.10):
    """
    Calculate Fair Value using Benjamin Graham's revised formula,
    adapted to the Brazilian market.

    Formula: (EPS × (8.5 + 2g) × 4.4) / Y

    Where:
    - EPS = Earnings per share
    - g = Expected growth rate (decimal, e.g. 0.10 for 10%)
    - Y = Long-term Brazilian interest rate (fixed at 10%)
    """
    try:
        if eps and eps > 0 and growth_rate is not None:
            # Cap growth rate at 25% as recommended by Graham and floor it at 0
            # (8.5 is the no-growth multiple; negative growth would make the value negative)
            g = max(0.0, min(growth_rate, 0.25))

            fair_value = (eps * (8.5 + 2 * g * 100) * 4.4) / (bond_yield * 100)
            return fair_value
    except Exception:
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

        # Get the reference date (last available historical data date)
        last_date = hist.index[-1].strftime('%Y-%m-%d')

        performance = {
            "current_price": create_currency_object(current_price, "real_time"),
            "day_high": create_currency_object(hist['High'].iloc[-1], "real_time"),
            "day_low": create_currency_object(hist['Low'].iloc[-1], "real_time"),
        }

        # 1 day change (if we have at least 2 days)
        if len(hist) >= 2:
            prev_close = hist['Close'].iloc[-2]
            performance["change_1d"] = create_percent_object(calculate_price_change(current_price, prev_close), "historical", last_date)

        # 5 days (1 week) change
        if len(hist) >= 6:
            price_5d_ago = hist['Close'].iloc[-6]
            performance["change_5d"] = create_percent_object(calculate_price_change(current_price, price_5d_ago), "historical", last_date)

        # 1 month change (approximately 21 trading days)
        if len(hist) >= 22:
            price_1m_ago = hist['Close'].iloc[-22]
            performance["change_1m"] = create_percent_object(calculate_price_change(current_price, price_1m_ago), "historical", last_date)

        # 3 months change (approximately 63 trading days)
        if len(hist) >= 64:
            price_3m_ago = hist['Close'].iloc[-64]
            performance["change_3m"] = create_percent_object(calculate_price_change(current_price, price_3m_ago), "historical", last_date)

        # 6 months change (approximately 126 trading days)
        if len(hist) >= 127:
            price_6m_ago = hist['Close'].iloc[-127]
            performance["change_6m"] = create_percent_object(calculate_price_change(current_price, price_6m_ago), "historical", last_date)

        # 52 weeks (1 year) change
        if len(hist) >= 2:
            price_52w_ago = hist['Close'].iloc[0]
            performance["change_52w"] = create_percent_object(calculate_price_change(current_price, price_52w_ago), "historical", last_date)

        # Year to Date (YTD) change
        current_year = datetime.now().year
        ytd_data = hist[hist.index.year == current_year]
        if not ytd_data.empty and len(ytd_data) > 1:
            price_ytd_start = ytd_data['Close'].iloc[0]
            performance["change_ytd"] = create_percent_object(calculate_price_change(current_price, price_ytd_start), "historical", last_date)

        # 52-week high and low
        week_52_high = hist['High'].max()
        week_52_low = hist['Low'].min()
        performance["week_52_high"] = create_currency_object(week_52_high, "historical", last_date)
        performance["week_52_low"] = create_currency_object(week_52_low, "historical", last_date)

        # Distance from 52-week high/low
        performance["distance_from_52w_high"] = create_percent_object(calculate_price_change(current_price, week_52_high), "calculated", last_date)
        performance["distance_from_52w_low"] = create_percent_object(calculate_price_change(current_price, week_52_low), "calculated", last_date)

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


def drop_empty_periods(df, key_row):
    """
    Drop period columns whose key row is NaN.

    Recent yfinance versions append the oldest period as a column where only
    auxiliary rows are filled, which turned revenue/EPS CAGR (and everything
    derived from them, like the Graham fair value and PEG) into None.

    :param df: Financial statement DataFrame (rows = items, columns = periods)
    :param key_row: Row that must be present for a period to be considered valid
    :return: DataFrame without the empty periods
    """
    try:
        if key_row in df.index:
            return df.loc[:, df.loc[key_row].notna()]
        return df.dropna(axis=1, how="all")
    except Exception:
        return df


def statement_eps(financials, period):
    """
    Get the trailing-twelve-month diluted EPS from the income statement.

    Annual: the most recent "Diluted EPS". Quarterly: the sum of the four most recent
    quarters, only when all four are present.

    :param financials: Income statement DataFrame
    :param period: "annual" or "quarterly"
    :return: EPS or None
    """
    try:
        if "Diluted EPS" not in financials.index:
            return None
        row = financials.loc["Diluted EPS"]
        if period == "quarterly":
            last_four = row.iloc[:4]
            if len(last_four) < 4 or last_four.isna().any():
                return None
            return float(last_four.sum())
        valid = row.dropna()
        return float(valid.iloc[0]) if len(valid) else None
    except Exception:
        return None


def resolve_eps(eps_info, eps_statement, threshold=EPS_DIVERGENCE_THRESHOLD, force_statement=False):
    """
    Choose between the EPS reported by Yahoo (trailingEps) and the one from the income statement.

    Yahoo's trailingEps is sometimes inconsistent (e.g. computed over a single share class), so when
    it diverges from the statement EPS by more than the threshold the statement value is used.

    :param eps_info: trailingEps from ticker.info
    :param eps_statement: Diluted EPS from the income statement
    :param threshold: Maximum relative divergence tolerated
    :param force_statement: Use the statement EPS whenever it exists (e.g. after a currency conversion)
    :return: Tuple (eps, source) where source is "info" or "statement"
    """
    if not eps_statement:
        return eps_info, "info"
    if eps_info is None or force_statement:
        return eps_statement, "statement"
    if abs(eps_info - eps_statement) / abs(eps_statement) > threshold:
        return eps_statement, "statement"
    return eps_info, "info"


def statement_book_value_per_share(financials, shareholders_equity):
    """
    Book value per share consistent with the statement EPS: equity / diluted average shares.

    :param financials: Income statement DataFrame
    :param shareholders_equity: Stockholders equity of the same period
    :return: Book value per share or None
    """
    try:
        if "Diluted Average Shares" not in financials.index or not shareholders_equity:
            return None
        shares = financials.loc["Diluted Average Shares"].dropna()
        if not len(shares) or shares.iloc[0] <= 0:
            return None
        return float(shareholders_equity) / float(shares.iloc[0])
    except Exception:
        return None


def get_fx_rate(from_currency, to_currency, reference_date=None):
    """
    Get the closing exchange rate from one currency to another.

    :param from_currency: Source currency code (e.g. "USD")
    :param to_currency: Target currency code (e.g. "BRL")
    :param reference_date: ISO date of the statement period; the closest earlier close is used
    :return: Tuple (rate, date) or (None, None) when unavailable
    """
    try:
        from datetime import timedelta
        pair = yf.Ticker(f"{from_currency}{to_currency}=X")
        if reference_date:
            end = datetime.fromisoformat(reference_date) + timedelta(days=1)
            hist = pair.history(start=(end - timedelta(days=8)).strftime("%Y-%m-%d"), end=end.strftime("%Y-%m-%d"))
        else:
            hist = pair.history(period="5d")
        if hist.empty:
            return None, None
        return float(hist["Close"].iloc[-1]), hist.index[-1].strftime("%Y-%m-%d")
    except Exception:
        return None, None


def scale_monetary_rows(df, factor):
    """
    Multiply the monetary rows of a statement by an exchange rate, leaving share counts and ratios alone.

    :param df: Financial statement DataFrame
    :param factor: Exchange rate
    :return: Scaled copy of the DataFrame
    """
    scaled = df.copy()
    for row in scaled.index:
        if not any(marker in str(row).lower() for marker in NON_MONETARY_ROW_MARKERS):
            scaled.loc[row] = scaled.loc[row] * factor
    return scaled


def is_non_positive(value):
    """
    Check whether a value exists and is zero or negative.

    :param value: Number or None
    :return: True when the value is present and not positive
    """
    return value is not None and value <= 0


def missing_reason(section, key, ctx):
    """
    Explain why a metric has no value, so the interface can say it instead of showing "N/A".

    :param section: Section name (profitability, debt, efficiency, cash_generation, valuation, growth)
    :param key: Metric key inside the section
    :param ctx: Dictionary with the inputs the metrics are computed from
    :return: One of the REASON_* constants
    """
    eps = ctx.get("eps")
    reason = REASON_DATA_UNAVAILABLE

    if section == "profitability" and key == "roe" and is_non_positive(ctx.get("equity")):
        reason = REASON_NEGATIVE_EQUITY
    elif section == "debt" and key == "net_debt_to_ebitda" and is_non_positive(ctx.get("ebitda")):
        reason = REASON_NOT_APPLICABLE
    elif section == "growth" and key in ("yoy_growth", "cagr"):
        minimum_periods = 2 if key == "yoy_growth" else 3
        if ctx.get("revenue_periods", 0) < minimum_periods:
            reason = REASON_INSUFFICIENT_HISTORY
    elif section == "valuation":
        if key == "pe_ratio" and is_non_positive(eps):
            reason = REASON_NEGATIVE_EPS
        elif key == "eps_cagr":
            if ctx.get("eps_history_len", 0) < 2:
                reason = REASON_INSUFFICIENT_HISTORY
            elif is_non_positive(ctx.get("eps_oldest")):
                reason = REASON_NEGATIVE_EPS
        elif key == "peg_ratio":
            if is_non_positive(eps):
                reason = REASON_NEGATIVE_EPS
            elif ctx.get("eps_cagr") is None:
                reason = missing_reason("valuation", "eps_cagr", ctx)
            elif ctx["eps_cagr"] <= 0:
                reason = REASON_NEGATIVE_GROWTH
        elif key == "ev_to_ebitda" and is_non_positive(ctx.get("ebitda")):
            reason = REASON_NOT_APPLICABLE
        elif key in ("dividend_yield", "dividend_rate", "payout_ratio") and not ctx.get("dividend_rate"):
            reason = REASON_NOT_APPLICABLE
        elif key == "graham_number":
            if is_non_positive(eps):
                reason = REASON_NEGATIVE_EPS
            elif is_non_positive(ctx.get("book_value")):
                reason = REASON_NEGATIVE_EQUITY
        elif key == "graham_fair_value":
            if is_non_positive(eps):
                reason = REASON_NEGATIVE_EPS
            elif eps and ctx.get("growth_estimate") is None:
                reason = REASON_INSUFFICIENT_HISTORY
        elif key == "margin_of_safety":
            reason = missing_reason("valuation", "graham_fair_value", ctx)
            if reason == REASON_INSUFFICIENT_HISTORY and ctx.get("graham_number"):
                reason = REASON_DATA_UNAVAILABLE
            if reason == REASON_DATA_UNAVAILABLE:
                reason = missing_reason("valuation", "graham_number", ctx)
    return reason


def placeholder_metric(metric_type, reason, reference_date=None):
    """
    Build the object that stands for a metric without value.

    :param metric_type: percent, currency, ratio or number
    :param reason: One of the REASON_* constants
    :param reference_date: Reference date of the data
    :return: Metric object with a null value and the reason
    """
    obj = {"value": None, "type": metric_type, "data_temporality": "calculated", "reason": reason}
    if reference_date:
        obj["reference_date"] = reference_date
    return obj


def fill_missing_metrics(result, ctx, reference_dates, specs=None, reason_fn=None):
    """
    Make every metric of every section present: a metric that could not be computed becomes an object
    with a null value and the reason, instead of a bare null or a missing key.

    :param result: Result dictionary being built (changed in place)
    :param ctx: Inputs used by the reason function
    :param reference_dates: Reference date per section
    :param specs: Mapping section -> {metric key: type}; defaults to the company metrics
    :param reason_fn: Function (section, key, ctx) -> reason; defaults to missing_reason
    """
    specs = METRIC_SPECS if specs is None else specs
    reason_fn = missing_reason if reason_fn is None else reason_fn

    for section, metrics in specs.items():
        data = result.setdefault(section, {})
        for key, metric_type in metrics.items():
            if data.get(key) is None:
                data[key] = placeholder_metric(metric_type, reason_fn(section, key, ctx), reference_dates.get(section))


def fill_missing_growth(result, ctx, reference_date):
    """
    Make the revenue growth metrics present, with the reason when they could not be computed.

    :param result: Result dictionary being built (changed in place)
    :param ctx: Inputs used by missing_reason
    :param reference_date: Reference date of the growth data
    """
    growth = result.setdefault("growth", {})
    revenue_growth = growth.get("revenue_growth") or {}
    for key in ("yoy_growth", "cagr"):
        if revenue_growth.get(key) is None:
            revenue_growth[key] = placeholder_metric("percent", missing_reason("growth", key, ctx), reference_date)
    revenue_growth.setdefault("years", ctx.get("revenue_periods", 0))
    growth["revenue_growth"] = revenue_growth


def classify_asset(symbol, info, has_statements):
    """
    Tell a company from a real estate fund (FII) or an ETF, since only companies have financial statements.

    :param symbol: Ticker symbol (e.g. FIIA11.SA)
    :param info: ticker.info from yfinance
    :param has_statements: True when yfinance returned financial statements
    :return: "company", "fund" or "etf"
    """
    quote_type = str(info.get("quoteType") or "").upper()
    if quote_type in ("ETF", "MUTUALFUND"):
        return ASSET_KIND_ETF

    upper = symbol.upper()
    is_brazilian_11 = upper.endswith(".SA") and bool(BRAZILIAN_FUND_SYMBOL.match(upper[:-3]))
    if is_brazilian_11 and str(info.get("industry") or "").startswith("REIT"):
        return ASSET_KIND_FUND
    if is_brazilian_11 and not has_statements and not info.get("sector"):
        return ASSET_KIND_ETF
    return ASSET_KIND_COMPANY


def monthly_dividends(dividends, today, months=DIVIDEND_MONTHS):
    """
    Sum the dividends per calendar month, for the last months including the current one.

    :param dividends: Series of dividends indexed by payment date (may be empty)
    :param today: Reference date (datetime)
    :param months: How many months to return
    :return: List of {"month": "YYYY-MM", "value": total} from the oldest to the current month
    """
    totals = {}
    try:
        for date, value in dividends.items():
            key = date.strftime("%Y-%m")
            totals[key] = totals.get(key, 0.0) + float(value)
    except Exception:
        totals = {}

    result = []
    for offset in range(months - 1, -1, -1):
        index = today.year * 12 + (today.month - 1) - offset
        key = f"{index // 12:04d}-{index % 12 + 1:02d}"
        result.append({"month": key, "value": round(totals.get(key, 0.0), 6)})
    return result


def fund_missing_reason(section, key, ctx):
    """
    Explain why a fund or ETF metric has no value.

    :param section: Section name (income or valuation)
    :param key: Metric key
    :param ctx: Dictionary with "kind" and "has_dividends"
    :return: One of the REASON_* constants
    """
    if section == "income" and not ctx.get("has_dividends"):
        return REASON_NOT_APPLICABLE
    if key == "price_to_book" and ctx.get("kind") == ASSET_KIND_ETF:
        return REASON_NOT_APPLICABLE
    return REASON_DATA_UNAVAILABLE


def build_fund_result(symbol, ticker, info, period, asset_kind):
    """
    Build the analysis of a fund or ETF: price performance, income and price-to-book, with no company statements.

    :param symbol: Ticker symbol
    :param ticker: yfinance Ticker object
    :param info: ticker.info
    :param period: "annual" or "quarterly"
    :param asset_kind: "fund" or "etf"
    :return: Result dictionary (same layout as the company analysis)
    """
    now = datetime.now()
    today = now.strftime("%Y-%m-%d")
    result = {
        "symbol": symbol,
        "company_name": safe_get(info, "longName", symbol),
        "sector": safe_get(info, "sector"),
        "industry": safe_get(info, "industry"),
        "currency": safe_get(info, "currency", DEFAULT_CURRENCY),
        "period_type": period,
        "last_updated": now.isoformat(),
        "schema_version": SCHEMA_VERSION,
        "asset_kind": asset_kind,
    }

    price_performance = get_price_performance(ticker)
    if price_performance:
        result["price_performance"] = price_performance

    price = safe_get(info, "currentPrice") or safe_get(info, "regularMarketPrice")
    if not price and price_performance and price_performance.get("current_price"):
        price = price_performance["current_price"]["value"]

    try:
        dividends = ticker.dividends
    except Exception:
        dividends = None
    monthly = monthly_dividends(dividends if dividends is not None else [], now)
    total_12m = sum(item["value"] for item in monthly)
    has_dividends = total_12m > 0

    if not price and not has_dividends:
        result["error"] = "Financial data not available"
        return result

    last_dividend_value = None
    last_dividend_date = None
    if dividends is not None and len(dividends) > 0:
        last_dividend_value = float(dividends.iloc[-1])
        last_dividend_date = dividends.index[-1].strftime("%Y-%m-%d")

    price_to_book = safe_get(info, "priceToBook")
    book_value = safe_get(info, "bookValue")
    if price_to_book is None and price and book_value:
        price_to_book = price / book_value

    result["income"] = {
        "dividend_yield": create_percent_object(
            (total_12m / price) * 100 if has_dividends and price else None, "calculated", today
        ),
        "last_dividend": create_currency_object(last_dividend_value, "historical", last_dividend_date),
        "dividends_12m_total": create_currency_object(total_12m if has_dividends else None, "calculated", today),
    }
    result["valuation"] = {
        "current_price": create_currency_object(price, "real_time"),
        "market_cap": create_currency_object(safe_get(info, "marketCap"), "real_time"),
        "price_to_book": create_ratio_object(price_to_book, "calculated", today),
    }

    fill_missing_metrics(
        result,
        {"kind": asset_kind, "has_dividends": has_dividends},
        {"income": today, "valuation": today},
        FUND_METRIC_SPECS,
        fund_missing_reason,
    )
    result["income"]["dividends_12m"] = monthly
    return result


def get_fundamental_data(symbol: str, period: str = "annual") -> dict:
    """
    Get fundamental data for a stock symbol

    :param symbol: Stock symbol (e.g., PETR4.SA for Petrobras)
    :param period: Period type - 'annual' or 'quarterly'
    :return: Dictionary with fundamental data
    """
    try:
        # Create ticker
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

        asset_kind = classify_asset(symbol, info, not financials.empty and not balance_sheet.empty)
        if asset_kind != ASSET_KIND_COMPANY:
            return build_fund_result(symbol, ticker, info, period, asset_kind)

        financials = drop_empty_periods(financials, "Total Revenue")
        balance_sheet = drop_empty_periods(balance_sheet, "Total Assets")
        cashflow = drop_empty_periods(cashflow, "Operating Cash Flow")

        price_currency = safe_get(info, "currency", DEFAULT_CURRENCY)
        financial_currency = safe_get(info, "financialCurrency", price_currency)
        fx_rate = None
        fx_date = None
        if financial_currency != price_currency and not financials.empty:
            fx_rate, fx_date = get_fx_rate(financial_currency, price_currency, financials.columns[0].strftime("%Y-%m-%d"))
            if fx_rate:
                financials = scale_monetary_rows(financials, fx_rate)
                balance_sheet = scale_monetary_rows(balance_sheet, fx_rate)
                cashflow = scale_monetary_rows(cashflow, fx_rate)

        result = {
            "symbol": symbol,
            "company_name": safe_get(info, "longName", symbol),
            "sector": safe_get(info, "sector"),
            "industry": safe_get(info, "industry"),
            "currency": safe_get(info, "currency", DEFAULT_CURRENCY),
            "period_type": period,
            "last_updated": datetime.now().isoformat(),
            "schema_version": SCHEMA_VERSION,
            "asset_kind": ASSET_KIND_COMPANY,
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
            eps_statement = statement_eps(financials, period)
            eps, eps_source = resolve_eps(
                safe_get(info, "trailingEps"), eps_statement, force_statement=bool(fx_rate)
            )
            pe_ratio = calculate_pe_ratio(current_price, eps) if current_price and eps else None
            earnings_yield = calculate_earnings_yield(eps, current_price) if current_price and eps else None
            fcf_yield = calculate_fcf_yield(free_cash_flow, market_cap) if free_cash_flow and market_cap else None

            eps_history = None
            try:
                if "Diluted EPS" in financials.index:
                    eps_series = financials.loc["Diluted EPS"].dropna()
                    eps_history = eps_series.head(5).tolist()
            except Exception:
                eps_history = None

            eps_cagr = calculate_eps_cagr(eps_history) if eps_history else None

            peg_ratio = None
            if pe_ratio and eps_cagr and eps_cagr > 0:
                peg_ratio = pe_ratio / eps_cagr

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
            if eps_source == "statement":
                book_value_per_share = (
                    statement_book_value_per_share(financials, shareholders_equity) or book_value_per_share
                )
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
            if fair_value_used and fair_value_used > 0 and current_price:
                margin_of_safety = ((fair_value_used - current_price) / fair_value_used) * 100

            result["valuation"] = {
                "current_price": create_currency_object(current_price, "real_time"),
                "market_cap": create_currency_object(market_cap, "real_time"),
                "enterprise_value": create_currency_object(enterprise_value, "calculated", balance_date),
                "eps": create_currency_object(eps, "historical", financials_date),
                "pe_ratio": create_ratio_object(pe_ratio, "calculated", financials_date),
                "peg_ratio": create_ratio_object(peg_ratio, "calculated", financials_date),
                "eps_cagr": create_percent_object(eps_cagr, "calculated", financials_date),
                "ev_to_ebitda": create_ratio_object(ev_to_ebitda, "calculated", financials_date),
                "earnings_yield": create_percent_object(earnings_yield, "calculated", financials_date),
                "fcf_yield": create_percent_object(fcf_yield, "calculated", cashflow_date),
                "dividend_yield": create_percent_object(dividend_yield, "calculated", financials_date),
                "dividend_rate": create_currency_object(dividend_rate, "historical", financials_date),
                "payout_ratio": create_percent_object(payout_ratio, "calculated", cashflow_date),
                "graham_number": create_currency_object(graham_number, "calculated", financials_date),
                "graham_fair_value": create_currency_object(graham_fair_value, "calculated", financials_date),
                "margin_of_safety": create_percent_object(margin_of_safety, "calculated", financials_date),
                "eps_source": eps_source,
                "financial_currency": financial_currency,
                "fx_rate": fx_rate,
                "fx_date": fx_date,
            }

            fill_missing_metrics(
                result,
                {
                    "eps": eps,
                    "eps_cagr": eps_cagr,
                    "eps_history_len": len(eps_history) if eps_history else 0,
                    "eps_oldest": eps_history[-1] if eps_history else None,
                    "book_value": book_value_per_share,
                    "equity": shareholders_equity,
                    "ebitda": ebitda,
                    "dividend_rate": dividend_rate,
                    "growth_estimate": growth_estimate,
                    "graham_number": graham_number,
                    "revenue_periods": min(5, len(financials.columns)),
                },
                {
                    "profitability": financials_date,
                    "debt": balance_date,
                    "efficiency": financials_date,
                    "cash_generation": cashflow_date,
                    "valuation": financials_date,
                },
            )
            fill_missing_growth(result, {"revenue_periods": min(5, len(financials.columns))}, financials_date)

        else:
            result["error"] = "Financial data not available"

        return result

    except Exception as e:
        if res.is_rate_limit(e):
            raise
        return {
            "symbol": symbol,
            **res.error_payload(e)
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
            writer.writerow(["Valuation", "EPS", extract_value(val.get("eps")), data.get("currency", "BRL")])
            writer.writerow(["Valuation", "P/E Ratio", extract_value(val.get("pe_ratio")), "x"])
            writer.writerow(["Valuation", "PEG Ratio", extract_value(val.get("peg_ratio")), "x"])
            writer.writerow(["Valuation", "EPS CAGR", extract_value(val.get("eps_cagr")), "%"])
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
            try:
                results[symbol] = retry_call(lambda: get_fundamental_data(symbol, args.period), symbol)
            except Exception as e:
                results[symbol] = {"symbol": symbol, **res.error_payload(e)}
        print(json.dumps(results, indent=2, ensure_ascii=False))

    elif args.format == "csv":
        for symbol in args.symbols:
            print(f"\n{'='*60}")
            print(f"Processing {symbol} ({args.period})...")
            print(f"{'='*60}")
            try:
                data = retry_call(lambda: get_fundamental_data(symbol, args.period), symbol)
            except Exception as e:
                data = {"symbol": symbol, **res.error_payload(e)}
            export_to_csv(symbol, data, args.output, args.period)

        print(f"\n{'='*60}")
        print(f"✓ Processing complete for {len(args.symbols)} symbol(s)")
        print(f"{'='*60}")


if __name__ == "__main__":
    main()
