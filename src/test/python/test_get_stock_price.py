# Filename: test_get_stock_price.py
# Created on: November 6, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
Contract tests for get_stock_price.py: the JSON the Kotlin side consumes, the call volume and the failure
cases that must never store a wrong price. No network access is used.
"""

import importlib.util
import logging
import sys
import unittest
from pathlib import Path
from unittest import mock

import pandas as pd

sys.dont_write_bytecode = True

SCRIPTS = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
sys.path.insert(0, str(SCRIPTS))

spec = importlib.util.spec_from_file_location("get_stock_price", SCRIPTS / "get_stock_price.py")
gsp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gsp)

res = gsp.res


def closes(columns):
    """A yf.download-like frame with MultiIndex columns ('Close', symbol)."""
    index = pd.date_range("2026-11-02", periods=3)
    frame = pd.DataFrame(columns, index=index)
    frame.columns = pd.MultiIndex.from_product([["Close"], frame.columns])
    return frame


class ParseArgumentsTest(unittest.TestCase):
    def test_symbols_with_and_without_currency(self):
        symbols, currencies = gsp.parse_arguments(["AAA:usd", "BBB", "AAA", "CCC:BRL"])
        self.assertEqual(symbols, ["AAA", "BBB", "CCC"])
        self.assertEqual(currencies, {"AAA": "USD", "CCC": "BRL"})


class CloseSeriesTest(unittest.TestCase):
    def test_multi_symbol_frame(self):
        data = closes({"AAA": [1.0, 2.0, 3.0], "BBB": [4.0, 5.0, None]})
        self.assertEqual(gsp.close_series(data, "AAA").iloc[-1], 3.0)
        self.assertEqual(gsp.close_series(data, "BBB").iloc[-1], 5.0)

    def test_single_symbol_multiindex_frame(self):
        data = closes({"AAA": [1.0, 2.0, 3.0]})
        self.assertEqual(gsp.close_series(data, "AAA").iloc[-1], 3.0)

    def test_flat_frame(self):
        data = pd.DataFrame({"Close": [1.0, 2.0]})
        self.assertEqual(gsp.close_series(data, "AAA").iloc[-1], 2.0)

    def test_missing_symbol_or_empty_frame(self):
        self.assertIsNone(gsp.close_series(closes({"AAA": [1.0, 2.0, 3.0], "BBB": [1.0, 2.0, 3.0]}), "ZZZ"))
        self.assertIsNone(gsp.close_series(pd.DataFrame(), "AAA"))
        self.assertIsNone(gsp.close_series(None, "AAA"))


class FetchPricesBatchTest(unittest.TestCase):
    def setUp(self):
        sleep = mock.patch.object(gsp.time, "sleep")
        sleep.start()
        self.addCleanup(sleep.stop)
        retry_sleep = mock.patch.object(res.time, "sleep")
        retry_sleep.start()
        self.addCleanup(retry_sleep.stop)

    def test_prices_are_read_in_chunks(self):
        symbols = [f"S{i}" for i in range(10)]
        frames = [
            closes({s: [1.0, 2.0, 3.0] for s in symbols[:8]}),
            closes({s: [1.0, 2.0, 7.0] for s in symbols[8:]}),
        ]
        with mock.patch.object(gsp.yf, "download", side_effect=frames) as download:
            prices, errors = gsp.fetch_prices_batch(symbols)
        self.assertEqual(download.call_count, 2)
        self.assertEqual(errors, {})
        self.assertEqual(prices["S0"], 3.0)
        self.assertEqual(prices["S9"], 7.0)

    def test_empty_answer_is_an_invalid_symbol_not_a_rate_limit(self):
        with mock.patch.object(gsp.yf, "download", return_value=pd.DataFrame()) as download:
            prices, errors = gsp.fetch_prices_batch(["XYZ"])
        self.assertEqual(download.call_count, 1)
        self.assertEqual(prices, {})
        self.assertEqual(errors["XYZ"]["error_kind"], "invalid_symbol")

    def test_one_invalid_symbol_does_not_hide_the_others(self):
        data = closes({"AAA": [1.0, 2.0, 3.0], "BBB": [None, None, None]})
        with mock.patch.object(gsp.yf, "download", return_value=data):
            prices, errors = gsp.fetch_prices_batch(["AAA", "BBB"])
        self.assertEqual(prices, {"AAA": 3.0})
        self.assertEqual(errors["BBB"]["error_kind"], "invalid_symbol")

    def test_rate_limit_exception_is_retried_once_then_reported(self):
        with mock.patch.object(gsp.yf, "download", side_effect=Exception("429 too many requests")) as download:
            prices, errors = gsp.fetch_prices_batch(["AAA", "BBB"])
        self.assertEqual(download.call_count, 2)
        self.assertEqual(prices, {})
        self.assertEqual({e["error_kind"] for e in errors.values()}, {"rate_limit"})

    def test_rate_limit_logged_by_yfinance_triggers_the_retry(self):
        def download(chunk, **kwargs):
            logging.getLogger("yfinance").error("['AAA']: YFRateLimitError('Too Many Requests. Rate limited.')")
            return pd.DataFrame()

        with mock.patch.object(gsp.yf, "download", side_effect=download) as mocked:
            prices, errors = gsp.fetch_prices_batch(["AAA"])
        self.assertEqual(mocked.call_count, 2)
        self.assertEqual(errors["AAA"]["error_kind"], "rate_limit")

    def test_other_error_logged_by_yfinance_is_kept_for_that_symbol_only(self):
        data = closes({"AAA": [1.0, 2.0, 3.0], "BBB": [None, None, None]})

        def download(chunk, **kwargs):
            logging.getLogger("yfinance").error("['BBB']: YFPricesMissingError('possibly delisted')")
            return data

        with mock.patch.object(gsp.yf, "download", side_effect=download) as mocked:
            prices, errors = gsp.fetch_prices_batch(["AAA", "BBB"])
        self.assertEqual(mocked.call_count, 1)
        self.assertEqual(prices, {"AAA": 3.0})
        self.assertEqual(errors["BBB"]["error_kind"], "invalid_symbol")
        self.assertIn("delisted", errors["BBB"]["error"])

    def test_after_a_rate_limit_no_more_chunks_are_requested(self):
        symbols = [f"S{i}" for i in range(20)]
        with mock.patch.object(gsp.yf, "download", side_effect=Exception("429")) as download:
            prices, errors = gsp.fetch_prices_batch(symbols)
        self.assertEqual(download.call_count, 2)
        self.assertEqual(len(errors), 20)
        self.assertEqual({e["error_kind"] for e in errors.values()}, {"rate_limit"})


class BuildResultTest(unittest.TestCase):
    def setUp(self):
        res._conversion_rates.clear()
        self.rates = mock.patch.object(res, "get_conversion_rate", side_effect=lambda c: {"BRL": 1.0, "USD": 5.0}[c])
        self.rates.start()
        self.addCleanup(self.rates.stop)

    def test_known_currency_converts_and_needs_no_lookup(self):
        result = gsp.build_result(["AAA"], {"AAA": 10.0}, {}, {"AAA": "USD"}, {})
        self.assertEqual(result, {"AAA": {"price": 50.0, "currency": "USD"}})

    def test_lookup_result_adds_currency_and_website(self):
        result = gsp.build_result(["AAA"], {"AAA": 10.0}, {}, {}, {"AAA": ("BRL", "https://a.example")})
        self.assertEqual(result["AAA"], {"price": 10.0, "currency": "BRL", "website": "https://a.example"})

    def test_failed_lookup_never_falls_back_to_usd_or_brl(self):
        failure = res.error_payload("Currency of AAA is unknown", kind="currency_unknown")
        result = gsp.build_result(["AAA"], {"AAA": 10.0}, {}, {}, {"AAA": failure})
        self.assertNotIn("price", result["AAA"])
        self.assertEqual(result["AAA"]["error_kind"], "currency_unknown")

    def test_unavailable_conversion_rate_is_an_error_not_a_price(self):
        with mock.patch.object(res, "get_conversion_rate", side_effect=Exception("down")):
            result = gsp.build_result(["AAA"], {"AAA": 10.0}, {}, {"AAA": "EUR"}, {})
        self.assertNotIn("price", result["AAA"])
        self.assertIn("EUR", result["AAA"]["error"])

    def test_symbol_without_price_keeps_its_error(self):
        error = res.error_payload("No price data available for XYZ", kind="invalid_symbol")
        result = gsp.build_result(["XYZ"], {}, {"XYZ": error}, {}, {})
        self.assertEqual(result["XYZ"], error)


class MainFlowTest(unittest.TestCase):
    def run_main(self, argv, download, ticker_info=None):
        output = []
        with mock.patch.object(sys, "argv", ["get_stock_price.py"] + argv), \
                mock.patch.object(gsp.yf, "download", side_effect=download), \
                mock.patch.object(gsp.time, "sleep"), \
                mock.patch.object(res.time, "sleep"), \
                mock.patch.object(res, "get_conversion_rate", return_value=1.0), \
                mock.patch.object(gsp.yf, "Ticker") as ticker, \
                mock.patch("builtins.print", side_effect=lambda *a, **k: output.append(a[0]) if not k else None):
            ticker.return_value.info = ticker_info or {}
            ticker.return_value.fast_info = {}
            gsp.main()
            self.ticker = ticker
        import json
        return json.loads(output[-1])

    def test_symbols_with_a_known_currency_make_no_info_call(self):
        data = closes({"AAA": [1.0, 2.0, 3.0], "BBB": [1.0, 2.0, 4.0]})
        result = self.run_main(["AAA:BRL", "BBB:BRL"], [data])
        self.assertEqual(result["AAA"]["price"], 3.0)
        self.assertEqual(result["BBB"]["price"], 4.0)
        self.ticker.assert_not_called()

    def test_symbol_without_currency_makes_exactly_one_lookup(self):
        data = closes({"AAA": [1.0, 2.0, 3.0]})
        result = self.run_main(["AAA"], [data], ticker_info={"currency": "BRL", "website": "https://a.example"})
        self.assertEqual(result["AAA"]["currency"], "BRL")
        self.assertEqual(result["AAA"]["website"], "https://a.example")
        self.assertEqual(self.ticker.call_count, 1)

    def test_failing_lookup_returns_no_price(self):
        data = closes({"AAA": [1.0, 2.0, 3.0]})
        result = self.run_main(["AAA"], [data], ticker_info={})
        self.assertNotIn("price", result["AAA"])
        self.assertEqual(result["AAA"]["error_kind"], "currency_unknown")


if __name__ == "__main__":
    unittest.main()
