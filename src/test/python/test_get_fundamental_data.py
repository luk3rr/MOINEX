"""
Contract tests for get_fundamental_data.py.

They feed the script with statements shaped like the ones yfinance 1.7.0 returns (including the
oldest period that only has auxiliary rows filled) and check the JSON the Kotlin side consumes.
No network access is used.
"""

import importlib.util
import sys
import unittest
from datetime import datetime
from pathlib import Path
from unittest import mock

import pandas as pd

sys.dont_write_bytecode = True

def datetime_of(year, month, day):
    return datetime(year, month, day)


NAN = float("nan")
SCRIPT = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts" / "get_fundamental_data.py"

spec = importlib.util.spec_from_file_location("get_fundamental_data", SCRIPT)
gfd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gfd)

DATES = [pd.Timestamp(f"{year}-12-31") for year in (2025, 2024, 2023, 2022, 2021)]


def statement(rows, dates=DATES):
    return pd.DataFrame(rows, index=dates).T


def financials_frame(revenues, eps, shares, net_income=19.6e9):
    return statement(
        {
            "Total Revenue": revenues,
            "Net Income": [net_income] * 4 + [NAN],
            "EBIT": [30e9] * 4 + [NAN],
            "EBITDA": [40e9] * 4 + [NAN],
            "Diluted EPS": eps,
            "Diluted Average Shares": shares,
            "Tax Provision": [5e9] * 4 + [NAN],
            "Tax Rate For Calcs": [0.34] * 5,
        }
    )


def balance_frame(equity=75.5e9):
    return statement(
        {
            "Total Assets": [200e9, 190e9, 180e9, 170e9, NAN],
            "Current Assets": [60e9] * 4 + [NAN],
            "Current Liabilities": [40e9] * 4 + [NAN],
            "Total Debt": [80e9] * 4 + [NAN],
            "Stockholders Equity": [equity] * 4 + [NAN],
            "Cash And Cash Equivalents": [20e9] * 4 + [NAN],
        }
    )


def cashflow_frame():
    return statement(
        {
            "Operating Cash Flow": [50e9] * 4 + [NAN],
            "Capital Expenditure": [-20e9] * 4 + [NAN],
            "Cash Dividends Paid": [-10e9] * 4 + [NAN],
        }
    )


class FakeTicker:
    def __init__(self, info, financials, balance, cashflow, dividends=None):
        self.dividends = dividends if dividends is not None else pd.Series(dtype=float)
        self.info = info
        self.financials = financials
        self.balance_sheet = balance
        self.cashflow = cashflow
        self.quarterly_financials = financials
        self.quarterly_balance_sheet = balance
        self.quarterly_cashflow = cashflow

    def history(self, *args, **kwargs):
        return pd.DataFrame()


def run(info, financials, balance=None, cashflow=None, period="annual", fx=None, symbol="TEST3.SA", dividends=None):
    ticker = FakeTicker(info, financials, balance if balance is not None else balance_frame(),
                        cashflow if cashflow is not None else cashflow_frame(), dividends)
    patches = [mock.patch.object(gfd.yf, "Ticker", return_value=ticker)]
    if fx is not None:
        patches.append(mock.patch.object(gfd, "get_fx_rate", return_value=fx))
    for patcher in patches:
        patcher.start()
    try:
        return gfd.get_fundamental_data(symbol, period)
    finally:
        mock.patch.stopall()


def info(**overrides):
    base = {"currency": "BRL", "financialCurrency": "BRL", "currentPrice": 40.0, "marketCap": 500e9,
            "enterpriseValue": 560e9, "trailingEps": 1.52, "bookValue": 5.86, "longName": "Test"}
    base.update(overrides)
    return base


class EmptyOldestPeriodTest(unittest.TestCase):
    def setUp(self):
        self.revenues = [110e9, 100e9, 95e9, 90e9, NAN]
        self.eps = [1.52, 1.40, 1.30, 1.20, NAN]
        self.shares = [12.89e9] * 4 + [NAN]

    def test_cagr_and_fair_value_survive_the_empty_oldest_column(self):
        result = run(info(), financials_frame(self.revenues, self.eps, self.shares))

        self.assertNotIn("error", result)
        cagr = result["growth"]["revenue_growth"]["cagr"]
        self.assertIsNotNone(cagr)
        self.assertAlmostEqual(cagr["value"], ((110 / 90) ** (1 / 3) - 1) * 100, places=4)
        self.assertIsNotNone(result["valuation"]["graham_fair_value"])
        self.assertIsNotNone(result["valuation"]["graham_number"])
        self.assertIsNotNone(result["valuation"]["peg_ratio"])
        self.assertIsNotNone(result["valuation"]["margin_of_safety"])

    def test_drop_empty_periods_keeps_only_periods_with_the_key_row(self):
        frame = financials_frame(self.revenues, self.eps, self.shares)

        trimmed = gfd.drop_empty_periods(frame, "Total Revenue")

        self.assertEqual(trimmed.shape[1], 4)
        self.assertNotIn(DATES[-1], list(trimmed.columns))


class GrahamFairValueTest(unittest.TestCase):
    def test_negative_growth_never_makes_the_fair_value_negative(self):
        fair_value = gfd.calculate_graham_fair_value(eps=2.0, growth_rate=-0.10)

        self.assertAlmostEqual(fair_value, 2.0 * 8.5 * 4.4 / 10)

    def test_growth_is_capped_at_25_percent(self):
        capped = gfd.calculate_graham_fair_value(eps=2.0, growth_rate=0.80)

        self.assertAlmostEqual(capped, gfd.calculate_graham_fair_value(eps=2.0, growth_rate=0.25))

    def test_declining_revenue_gives_a_bounded_margin_of_safety(self):
        revenues = [80e9, 90e9, 95e9, 100e9, NAN]
        result = run(info(), financials_frame(revenues, [1.52, 1.4, 1.3, 1.2, NAN], [12.89e9] * 4 + [NAN]))

        valuation = result["valuation"]
        self.assertGreater(valuation["graham_fair_value"]["value"], 0)
        self.assertLessEqual(valuation["margin_of_safety"]["value"], 100)


class EpsResolutionTest(unittest.TestCase):
    def test_statement_eps_replaces_a_divergent_yahoo_eps(self):
        eps, source = gfd.resolve_eps(10.49, 1.52)

        self.assertEqual((eps, source), (1.52, "statement"))

    def test_yahoo_eps_is_kept_when_it_is_close_to_the_statement(self):
        eps, source = gfd.resolve_eps(4.14, 4.01)

        self.assertEqual((eps, source), (4.14, "info"))

    def test_yahoo_eps_is_kept_when_the_statement_has_none(self):
        self.assertEqual(gfd.resolve_eps(1.2, None), (1.2, "info"))

    def test_divergent_eps_is_reported_in_the_json_and_drives_graham(self):
        revenues = [110e9, 100e9, 95e9, 90e9, NAN]
        result = run(
            info(trailingEps=10.49, bookValue=37.316),
            financials_frame(revenues, [1.52, 1.40, 1.30, 1.20, NAN], [12.89e9] * 4 + [NAN]),
        )

        valuation = result["valuation"]
        self.assertEqual(valuation["eps_source"], "statement")
        self.assertAlmostEqual(valuation["eps"]["value"], 1.52)
        expected_bvps = 75.5e9 / 12.89e9
        self.assertAlmostEqual(valuation["graham_number"]["value"], (22.5 * 1.52 * expected_bvps) ** 0.5, places=4)

    def test_close_eps_is_reported_as_info(self):
        revenues = [110e9, 100e9, 95e9, 90e9, NAN]
        result = run(
            info(trailingEps=1.55),
            financials_frame(revenues, [1.52, 1.40, 1.30, 1.20, NAN], [12.89e9] * 4 + [NAN]),
        )

        self.assertEqual(result["valuation"]["eps_source"], "info")
        self.assertAlmostEqual(result["valuation"]["eps"]["value"], 1.55)

    def test_quarterly_statement_eps_needs_four_quarters(self):
        quarters = [DATES[0] - pd.DateOffset(months=3 * i) for i in range(5)]
        full = statement({"Diluted EPS": [0.4, 0.4, 0.4, 0.4, 0.4]}, quarters)
        gappy = statement({"Diluted EPS": [0.4, 0.4, NAN, 0.4, 0.4]}, quarters)

        self.assertAlmostEqual(gfd.statement_eps(full, "quarterly"), 1.6)
        self.assertIsNone(gfd.statement_eps(gappy, "quarterly"))


class CurrencyConversionTest(unittest.TestCase):
    def test_statements_in_another_currency_are_converted_and_reported(self):
        revenues = [110e9, 100e9, 95e9, 90e9, NAN]
        result = run(
            info(financialCurrency="USD", trailingEps=0.30),
            financials_frame(revenues, [0.30, 0.28, 0.26, 0.24, NAN], [12.89e9] * 4 + [NAN]),
            fx=(5.0, "2025-12-31"),
        )

        valuation = result["valuation"]
        self.assertEqual(valuation["financial_currency"], "USD")
        self.assertEqual(valuation["fx_rate"], 5.0)
        self.assertEqual(valuation["fx_date"], "2025-12-31")
        self.assertEqual(valuation["eps_source"], "statement")
        self.assertAlmostEqual(valuation["eps"]["value"], 1.5)

    def test_ratios_do_not_change_with_the_conversion(self):
        revenues = [110e9, 100e9, 95e9, 90e9, NAN]
        frame = financials_frame(revenues, [1.5, 1.4, 1.3, 1.2, NAN], [12.89e9] * 4 + [NAN])
        same = run(info(), frame)
        converted = run(info(financialCurrency="USD"), frame, fx=(5.0, "2025-12-31"))

        self.assertAlmostEqual(
            same["profitability"]["roe"]["value"], converted["profitability"]["roe"]["value"], places=6
        )

    def test_share_counts_are_not_scaled(self):
        scaled = gfd.scale_monetary_rows(
            statement({"Total Revenue": [10.0] * 5, "Diluted Average Shares": [1000.0] * 5, "Tax Rate For Calcs": [0.3] * 5}),
            5.0,
        )

        self.assertEqual(scaled.loc["Total Revenue"].iloc[0], 50.0)
        self.assertEqual(scaled.loc["Diluted Average Shares"].iloc[0], 1000.0)
        self.assertEqual(scaled.loc["Tax Rate For Calcs"].iloc[0], 0.3)

    def test_same_currency_leaves_the_fx_fields_empty(self):
        revenues = [110e9, 100e9, 95e9, 90e9, NAN]
        result = run(info(), financials_frame(revenues, [1.5, 1.4, 1.3, 1.2, NAN], [12.89e9] * 4 + [NAN]))

        self.assertIsNone(result["valuation"]["fx_rate"])
        self.assertEqual(result["valuation"]["financial_currency"], "BRL")


class MissingMetricReasonTest(unittest.TestCase):
    REVENUES = [110e9, 100e9, 95e9, 90e9, NAN]
    EPS = [1.52, 1.40, 1.30, 1.20, NAN]
    SHARES = [12.89e9] * 4 + [NAN]

    def reason(self, result, section, key):
        metric = result[section][key]
        self.assertIsNone(metric["value"], f"{section}.{key} should have no value")
        return metric["reason"]

    def test_every_catalogued_metric_is_present_with_a_value_or_a_reason(self):
        result = run(info(), financials_frame(self.REVENUES, self.EPS, self.SHARES))

        self.assertEqual(result["schema_version"], 2)
        self.assertEqual(result["asset_kind"], "company")
        for section, metrics in gfd.METRIC_SPECS.items():
            for key in metrics:
                metric = result[section][key]
                self.assertIsNotNone(metric, f"{section}.{key} is missing")
                self.assertTrue(metric["value"] is not None or metric.get("reason"), f"{section}.{key}")

    def test_negative_eps_explains_graham_and_pe(self):
        result = run(info(trailingEps=-1.0), financials_frame(self.REVENUES, [-1.0, -0.8, -0.5, -0.2, NAN], self.SHARES))

        self.assertEqual(self.reason(result, "valuation", "graham_number"), "NEGATIVE_EPS")
        self.assertEqual(self.reason(result, "valuation", "graham_fair_value"), "NEGATIVE_EPS")
        self.assertEqual(self.reason(result, "valuation", "pe_ratio"), "NEGATIVE_EPS")
        self.assertEqual(self.reason(result, "valuation", "margin_of_safety"), "NEGATIVE_EPS")
        self.assertEqual(self.reason(result, "valuation", "peg_ratio"), "NEGATIVE_EPS")

    def test_declining_eps_explains_the_missing_peg(self):
        result = run(info(), financials_frame(self.REVENUES, [1.0, 1.2, 1.3, 1.5, NAN], self.SHARES))

        self.assertEqual(self.reason(result, "valuation", "peg_ratio"), "NEGATIVE_GROWTH")

    def test_short_history_explains_the_missing_cagr(self):
        two_years = statement(
            {"Total Revenue": [110e9, 100e9], "Net Income": [19e9, 18e9], "EBIT": [30e9, 29e9], "EBITDA": [40e9, 39e9],
             "Diluted EPS": [1.5, 1.4], "Diluted Average Shares": [12.89e9] * 2},
            DATES[:2],
        )
        balance = statement(
            {"Total Assets": [200e9, 190e9], "Current Assets": [60e9] * 2, "Current Liabilities": [40e9] * 2,
             "Total Debt": [80e9] * 2, "Stockholders Equity": [75e9] * 2, "Cash And Cash Equivalents": [20e9] * 2},
            DATES[:2],
        )

        result = run(info(), two_years, balance)

        self.assertEqual(self.reason(result["growth"], "revenue_growth", "cagr"), "INSUFFICIENT_HISTORY")
        self.assertIsNotNone(result["growth"]["revenue_growth"]["yoy_growth"]["value"])

    def test_single_period_explains_eps_cagr_and_revenue_growth(self):
        one_year = statement(
            {"Total Revenue": [110e9], "Net Income": [19e9], "EBIT": [30e9], "EBITDA": [40e9], "Diluted EPS": [1.5],
             "Diluted Average Shares": [12.89e9]},
            DATES[:1],
        )
        balance = statement(
            {"Total Assets": [200e9], "Current Assets": [60e9], "Current Liabilities": [40e9], "Total Debt": [80e9],
             "Stockholders Equity": [75e9], "Cash And Cash Equivalents": [20e9]},
            DATES[:1],
        )

        result = run(info(), one_year, balance)

        self.assertEqual(self.reason(result, "valuation", "eps_cagr"), "INSUFFICIENT_HISTORY")
        self.assertEqual(self.reason(result["growth"], "revenue_growth", "yoy_growth"), "INSUFFICIENT_HISTORY")

    def test_negative_equity_explains_roe(self):
        result = run(info(), financials_frame(self.REVENUES, self.EPS, self.SHARES), balance_frame(equity=-5e9))

        self.assertEqual(self.reason(result, "profitability", "roe"), "NEGATIVE_EQUITY")

    def test_company_without_dividends_is_not_applicable(self):
        result = run(info(), financials_frame(self.REVENUES, self.EPS, self.SHARES))

        self.assertEqual(self.reason(result, "valuation", "dividend_rate"), "NOT_APPLICABLE")
        self.assertEqual(self.reason(result, "valuation", "dividend_yield"), "NOT_APPLICABLE")

    def test_missing_cash_flow_keeps_the_section_with_reasons(self):
        empty_cashflow = pd.DataFrame()
        result = run(info(), financials_frame(self.REVENUES, self.EPS, self.SHARES), cashflow=empty_cashflow)

        for key in gfd.METRIC_SPECS["cash_generation"]:
            self.assertEqual(self.reason(result, "cash_generation", key), "DATA_UNAVAILABLE")

    def test_reasons_carry_the_reference_date(self):
        result = run(info(trailingEps=-1.0), financials_frame(self.REVENUES, [-1.0, -0.8, -0.5, -0.2, NAN], self.SHARES))

        self.assertEqual(result["valuation"]["graham_number"]["reference_date"], "2025-12-31")


def monthly_payments(amount, months_back=range(0, 12)):
    now = pd.Timestamp.now(tz="America/Sao_Paulo")
    dates = [(now - pd.DateOffset(months=m)).replace(day=5) for m in months_back]
    return pd.Series([amount] * len(dates), index=pd.DatetimeIndex(sorted(dates)))


class FundAndEtfTest(unittest.TestCase):
    EMPTY = pd.DataFrame()

    def fund(self, symbol="FIIA11.SA", **info_overrides):
        base = {"currency": "BRL", "quoteType": "EQUITY", "sector": "Real Estate", "industry": "REIT - Diversified",
                "currentPrice": 150.0, "priceToBook": 0.95, "marketCap": 9e9, "longName": "FII Test"}
        base.update(info_overrides)
        return run(base, self.EMPTY, self.EMPTY, self.EMPTY, symbol=symbol, dividends=monthly_payments(1.17))

    def test_real_estate_fund_gets_income_and_book_value_instead_of_an_error(self):
        result = self.fund()

        self.assertNotIn("error", result)
        self.assertEqual(result["asset_kind"], "fund")
        self.assertEqual(result["schema_version"], 2)
        income = result["income"]
        self.assertAlmostEqual(income["dividend_yield"]["value"], 1.17 * 12 / 150.0 * 100, places=3)
        self.assertAlmostEqual(income["dividends_12m_total"]["value"], 1.17 * 12, places=3)
        self.assertAlmostEqual(income["last_dividend"]["value"], 1.17)
        self.assertEqual(len(income["dividends_12m"]), 12)
        self.assertEqual(result["valuation"]["price_to_book"]["value"], 0.95)

    def test_the_monthly_series_ends_in_the_current_month(self):
        result = self.fund()

        self.assertEqual(result["income"]["dividends_12m"][-1]["month"], pd.Timestamp.now().strftime("%Y-%m"))

    def test_price_to_book_is_derived_from_the_book_value_when_missing(self):
        result = self.fund(priceToBook=None, bookValue=100.0)

        self.assertAlmostEqual(result["valuation"]["price_to_book"]["value"], 1.5)

    def test_missing_price_to_book_of_a_fund_is_data_unavailable(self):
        result = self.fund(priceToBook=None)

        self.assertEqual(result["valuation"]["price_to_book"]["reason"], "DATA_UNAVAILABLE")

    def test_brazilian_etf_without_dividends_has_no_income_and_no_book_value(self):
        info = {"currency": "BRL", "quoteType": "EQUITY", "currentPrice": 120.0, "longName": "ETF Test"}
        result = run(info, self.EMPTY, self.EMPTY, self.EMPTY, symbol="BOVA11.SA")

        self.assertNotIn("error", result)
        self.assertEqual(result["asset_kind"], "etf")
        self.assertEqual(result["income"]["dividend_yield"]["reason"], "NOT_APPLICABLE")
        self.assertEqual(result["income"]["last_dividend"]["reason"], "NOT_APPLICABLE")
        self.assertEqual(result["valuation"]["price_to_book"]["reason"], "NOT_APPLICABLE")
        self.assertEqual(result["income"]["dividends_12m"], [{"month": m["month"], "value": 0.0} for m in result["income"]["dividends_12m"]])
        self.assertEqual(result["valuation"]["current_price"]["value"], 120.0)

    def test_a_fund_with_no_price_and_no_dividends_keeps_the_unavailable_error(self):
        info = {"currency": "BRL", "quoteType": "EQUITY", "longName": "Delisted"}
        result = run(info, self.EMPTY, self.EMPTY, self.EMPTY, symbol="FIIB11.SA")

        self.assertEqual(result["error"], "Financial data not available")
        self.assertNotIn("income", result)

    def test_us_etf_is_recognised_by_its_quote_type(self):
        self.assertEqual(gfd.classify_asset("SPY", {"quoteType": "ETF"}, False), "etf")

    def test_a_brazilian_unit_with_statements_stays_a_company(self):
        info = {"quoteType": "EQUITY", "sector": "Utilities", "industry": "Utilities - Regulated Electric"}

        self.assertEqual(gfd.classify_asset("UNTA11.SA", info, True), "company")

    def test_a_foreign_reit_with_statements_stays_a_company(self):
        info = {"quoteType": "EQUITY", "sector": "Real Estate", "industry": "REIT - Retail"}

        self.assertEqual(gfd.classify_asset("O", info, True), "company")

    def test_a_brazilian_11_without_statements_or_sector_is_an_etf(self):
        self.assertEqual(gfd.classify_asset("IVVB11.SA", {"quoteType": "EQUITY"}, False), "etf")

    def test_monthly_dividends_sums_payments_of_the_same_month(self):
        index = pd.DatetimeIndex(["2026-09-03", "2026-09-20", "2026-10-05"], tz="America/Sao_Paulo")
        dividends = pd.Series([1.0, 0.5, 2.0], index=index)

        months = gfd.monthly_dividends(dividends, datetime_of(2026, 10, 20), months=3)

        self.assertEqual(months, [{"month": "2026-08", "value": 0.0}, {"month": "2026-09", "value": 1.5},
                                  {"month": "2026-10", "value": 2.0}])

    def test_monthly_dividends_crosses_the_year_boundary(self):
        months = gfd.monthly_dividends([], datetime_of(2026, 2, 10), months=4)

        self.assertEqual([m["month"] for m in months], ["2025-11", "2025-12", "2026-01", "2026-02"])


if __name__ == "__main__":
    unittest.main()
