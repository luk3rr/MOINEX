# Filename: test_get_stock_price_history.py
# Created on: November 6, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
Contract tests for get_stock_price_history.py: the batch and single outputs, the request volume (one download
per chunk, never period=max) and the failures that must never store wrong prices. No network access is used.
"""

import importlib.util
import json
import logging
import sys
import unittest
from datetime import date, timedelta
from pathlib import Path
from unittest import mock

import pandas as pd

sys.dont_write_bytecode = True

SCRIPTS = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
sys.path.insert(0, str(SCRIPTS))

spec = importlib.util.spec_from_file_location("get_stock_price_history", SCRIPTS / "get_stock_price_history.py")
gsph = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gsph)

res = gsph.res


def frame(columns, start="2026-01-26", periods=6):
    index = pd.date_range(start, periods=periods)
    data = pd.DataFrame(columns, index=index)
    data.columns = pd.MultiIndex.from_product([["Close"], data.columns])
    return data


def item(symbol, start="2026-01-26", end="2026-01-31", currency="BRL", dates=None):
    return {
        "symbol": symbol,
        "start": date.fromisoformat(start),
        "end": date.fromisoformat(end),
        "currency": currency,
        "dates": dates,
    }


class PatchedTest(unittest.TestCase):
    def setUp(self):
        for target in (mock.patch.object(gsph.time, "sleep"), mock.patch.object(res.time, "sleep")):
            target.start()
            self.addCleanup(target.stop)
        res._conversion_rates.clear()


class ParseBatchTest(unittest.TestCase):
    def test_items_are_parsed_with_defaults(self):
        items = gsph.parse_batch(json.dumps([
            {"symbol": "AAA", "start": "2026-01-01", "end": "2026-02-01", "currency": "usd", "dates": ["2026-01-05"]},
            {"symbol": "BBB", "start": "2026-01-01"},
        ]))
        self.assertEqual(items[0]["start"], date(2026, 1, 1))
        self.assertEqual(items[0]["currency"], "usd")
        self.assertEqual(items[1]["end"], date.today())
        self.assertIsNone(items[1]["currency"])


class SliceRangeTest(unittest.TestCase):
    def test_both_ends_are_inclusive(self):
        series = gsph.close_series(frame({"AAA": [1, 2, 3, 4, 5, 6]}), "AAA")
        kept = gsph.slice_range(series, date(2026, 1, 27), date(2026, 1, 29))
        self.assertEqual(list(kept), [2, 3, 4])

    def test_nothing_left_is_none(self):
        series = gsph.close_series(frame({"AAA": [1, 2, 3]}, periods=3), "AAA")
        self.assertIsNone(gsph.slice_range(series, date(2027, 1, 1), date(2027, 1, 5)))
        self.assertIsNone(gsph.slice_range(None, date(2026, 1, 1), date(2026, 1, 5)))


class FetchClosesTest(PatchedTest):
    def test_ten_symbols_cost_two_downloads(self):
        symbols = [f"S{i}" for i in range(10)]
        items = [item(s) for s in symbols]
        data = frame({s: [1, 2, 3, 4, 5, 6] for s in symbols})
        with mock.patch.object(gsph.yf, "download", return_value=data) as download:
            closes, errors = gsph.fetch_closes(items)
        self.assertEqual(download.call_count, 2)
        self.assertEqual(errors, {})
        self.assertEqual(set(closes), set(symbols))

    def test_no_download_ever_asks_for_the_max_period(self):
        with mock.patch.object(gsph.yf, "download", return_value=pd.DataFrame()) as download:
            gsph.fetch_closes([item("AAA", start="2026-01-01", end=date.today().isoformat())])
        for call in download.call_args_list:
            self.assertNotIn(call.kwargs.get("period"), ("max", "365d", "1y"))

    def test_each_item_keeps_only_its_own_range(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6], "BBB": [10, 20, 30, 40, 50, 60]})
        items = [item("AAA", "2026-01-26", "2026-01-27"), item("BBB", "2026-01-28", "2026-01-31")]
        with mock.patch.object(gsph.yf, "download", return_value=data) as download:
            closes, _ = gsph.fetch_closes(items)
        self.assertEqual(download.call_args.kwargs["start"], "2026-01-26")
        self.assertEqual(download.call_args.kwargs["end"], "2026-01-31")
        self.assertEqual(list(closes["AAA"]), [1, 2])
        self.assertEqual(list(closes["BBB"]), [30, 40, 50, 60])

    def test_symbol_without_data_is_an_invalid_symbol_and_the_others_survive(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6], "BBB": [None] * 6})
        with mock.patch.object(gsph.yf, "download", return_value=data) as download:
            closes, errors = gsph.fetch_closes([item("AAA"), item("BBB")])
        self.assertEqual(download.call_count, 1)
        self.assertIn("AAA", closes)
        self.assertEqual(errors["BBB"]["error_kind"], "invalid_symbol")
        self.assertIn("No data found for BBB", errors["BBB"]["error"])

    def test_recent_empty_symbols_get_one_extra_request_with_the_short_period(self):
        today = date.today()
        recent = item("AAA", start=(today - timedelta(days=3)).isoformat(), end=today.isoformat())
        fallback = pd.DataFrame(
            {"Close": [5.0, 6.0]}, index=pd.to_datetime([today - timedelta(days=1), today])
        )
        fallback.columns = pd.MultiIndex.from_product([["Close"], ["AAA"]])
        with mock.patch.object(gsph.yf, "download", side_effect=[pd.DataFrame(), fallback]) as download:
            closes, errors = gsph.fetch_closes([recent])
        self.assertEqual(download.call_count, 2)
        self.assertEqual(download.call_args_list[1].kwargs["period"], "15d")
        self.assertEqual(errors, {})
        self.assertEqual(list(closes["AAA"]), [5.0, 6.0])

    def test_old_empty_ranges_get_no_extra_request(self):
        with mock.patch.object(gsph.yf, "download", return_value=pd.DataFrame()) as download:
            closes, errors = gsph.fetch_closes([item("AAA", "2020-01-01", "2020-02-01")])
        self.assertEqual(download.call_count, 1)
        self.assertEqual(errors["AAA"]["error_kind"], "invalid_symbol")

    def test_rate_limit_stops_the_remaining_chunks(self):
        items = [item(f"S{i}") for i in range(20)]
        with mock.patch.object(gsph.yf, "download", side_effect=Exception("429 too many requests")) as download:
            closes, errors = gsph.fetch_closes(items)
        self.assertEqual(download.call_count, 2)
        self.assertEqual(closes, {})
        self.assertEqual({e["error_kind"] for e in errors.values()}, {"rate_limit"})
        self.assertEqual(len(errors), 20)

    def test_rate_limit_logged_by_yfinance_is_detected(self):
        def download(symbols, **kwargs):
            logging.getLogger("yfinance").error("['AAA']: YFRateLimitError('Too Many Requests')")
            return pd.DataFrame()

        with mock.patch.object(gsph.yf, "download", side_effect=download):
            closes, errors = gsph.fetch_closes([item("AAA")])
        self.assertEqual(errors["AAA"]["error_kind"], "rate_limit")


class BuildPricesTest(unittest.TestCase):
    def test_month_ends_and_specific_dates(self):
        index = pd.to_datetime(["2026-09-29", "2026-09-30", "2026-10-01", "2026-10-02", "2026-10-30"])
        closes = pd.Series([1.0, 2.0, 3.0, 4.0, 5.0], index=index)
        prices = gsph.build_prices(closes, ["2026-10-01"], 2.0)
        self.assertEqual(
            [(p["date"], p["price"], p["is_month_end"]) for p in prices],
            [("2026-09-30", 4.0, True), ("2026-10-01", 6.0, False), ("2026-10-30", 10.0, True)],
        )

    def test_without_specific_dates_every_day_is_kept(self):
        index = pd.to_datetime(["2026-10-01", "2026-10-02"])
        prices = gsph.build_prices(pd.Series([1.0, 2.0], index=index), [], 1.0)
        self.assertEqual(len(prices), 2)


class BuildResultsTest(PatchedTest):
    def test_given_currency_makes_no_info_call_and_converts(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6]})
        with mock.patch.object(gsph.yf, "download", return_value=data), \
                mock.patch.object(gsph.yf, "Ticker") as ticker, \
                mock.patch.object(res, "get_conversion_rate", return_value=5.0):
            results = gsph.build_results([item("AAA", currency="USD")])
        ticker.assert_not_called()
        self.assertEqual(results["AAA"]["currency"], "USD")
        self.assertEqual(results["AAA"]["conversion_rate"], 5.0)
        self.assertEqual(results["AAA"]["prices"][0]["price"], 5.0)

    def test_unknown_currency_is_an_error_never_brl(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6]})
        with mock.patch.object(gsph.yf, "download", return_value=data), \
                mock.patch.object(gsph.yf, "Ticker") as ticker:
            ticker.return_value.info = {}
            ticker.return_value.fast_info = {}
            results = gsph.build_results([item("AAA", currency=None)])
        self.assertNotIn("prices", results["AAA"])
        self.assertEqual(results["AAA"]["error_kind"], "currency_unknown")

    def test_unavailable_conversion_rate_is_an_error_not_a_price(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6]})
        with mock.patch.object(gsph.yf, "download", return_value=data), \
                mock.patch.object(res, "get_conversion_rate", side_effect=Exception("down")):
            results = gsph.build_results([item("AAA", currency="EUR")])
        self.assertNotIn("prices", results["AAA"])
        self.assertIn("EUR", results["AAA"]["error"])

    def test_one_failing_symbol_does_not_hide_the_others(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6], "BBB": [None] * 6})
        with mock.patch.object(gsph.yf, "download", return_value=data):
            results = gsph.build_results([item("AAA"), item("BBB")])
        self.assertIn("prices", results["AAA"])
        self.assertIn("error", results["BBB"])


class MainTest(PatchedTest):
    def run_main(self, argv, data):
        out = []
        err = []

        def fake_print(*args, **kwargs):
            (err if kwargs.get("file") is sys.stderr else out).append(args[0])

        with mock.patch.object(sys, "argv", ["get_stock_price_history.py"] + argv), \
                mock.patch.object(gsph.yf, "download", return_value=data), \
                mock.patch.object(gsph.yf, "Ticker") as ticker, \
                mock.patch.object(res, "get_conversion_rate", return_value=1.0), \
                mock.patch("builtins.print", side_effect=fake_print):
            ticker.return_value.info = {"currency": "BRL"}
            try:
                gsph.main()
                code = 0
            except SystemExit as e:
                code = e.code
        return json.loads(out[-1]) if out else None, err, code

    def test_batch_prints_results_keyed_by_symbol(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6], "BBB": [None] * 6})
        batch = json.dumps([
            {"symbol": "AAA", "start": "2026-01-26", "end": "2026-01-31", "currency": "BRL"},
            {"symbol": "BBB", "start": "2026-01-26", "end": "2026-01-31", "currency": "BRL"},
        ])
        output, _, code = self.run_main(["--batch", batch], data)
        self.assertEqual(code, 0)
        self.assertIn("prices", output["results"]["AAA"])
        self.assertEqual(output["results"]["BBB"]["error_kind"], "invalid_symbol")

    def test_single_symbol_keeps_the_legacy_shape(self):
        data = frame({"AAA": [1, 2, 3, 4, 5, 6]})
        output, _, code = self.run_main(["AAA", "2026-01-26", "2026-01-31"], data)
        self.assertEqual(code, 0)
        self.assertEqual(set(output), {"symbol", "currency", "conversion_rate", "prices"})

    def test_single_symbol_failure_exits_1_and_tags_stderr_with_the_kind(self):
        output, err, code = self.run_main(["ZZZ", "2020-01-01", "2020-01-31"], pd.DataFrame())
        self.assertEqual(code, 1)
        self.assertEqual(output["error_kind"], "invalid_symbol")
        self.assertTrue(any("error_kind=invalid_symbol" in line for line in err))

    def test_invalid_batch_is_reported(self):
        output, _, code = self.run_main(["--batch", "not json"], pd.DataFrame())
        self.assertEqual(code, 1)
        self.assertIn("Invalid batch", output["error"])


if __name__ == "__main__":
    unittest.main()
