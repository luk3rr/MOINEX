# Filename: test_yf_resilience.py
# Created on: November 6, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
Tests for yf_resilience.py, the module shared by the scripts that talk to Yahoo Finance.
No network access is used.
"""

import importlib.util
import sys
import unittest
from pathlib import Path
from unittest import mock

sys.dont_write_bytecode = True

SCRIPTS = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
spec = importlib.util.spec_from_file_location("yf_resilience", SCRIPTS / "yf_resilience.py")
res = importlib.util.module_from_spec(spec)
spec.loader.exec_module(res)


class YFRateLimitError(Exception):
    pass


class YFTickerMissingError(Exception):
    pass


class ClassifyErrorTest(unittest.TestCase):
    def test_rate_limit_by_type_name(self):
        self.assertEqual(res.classify_error(YFRateLimitError("blocked")), "rate_limit")

    def test_rate_limit_by_message(self):
        for text in ("HTTP Error 429", "Too Many Requests. Rate limited", "rate limit hit"):
            self.assertEqual(res.classify_error(Exception(text)), "rate_limit", text)

    def test_invalid_symbol(self):
        self.assertEqual(res.classify_error(YFTickerMissingError("x")), "invalid_symbol")
        self.assertEqual(res.classify_error(Exception("No data found, symbol may be delisted")), "invalid_symbol")

    def test_other_errors(self):
        self.assertEqual(res.classify_error(ValueError("boom")), "error")

    def test_plain_text_is_classified_too(self):
        self.assertEqual(res.classify_error("429 Client Error"), "rate_limit")

    def test_error_payload_carries_the_kind(self):
        payload = res.error_payload(Exception("429"), symbol="AAA")
        self.assertEqual(payload, {"error": "429", "error_kind": "rate_limit", "symbol": "AAA"})
        self.assertEqual(res.error_payload("x", kind="currency_unknown")["error_kind"], "currency_unknown")


class RetryCallTest(unittest.TestCase):
    def test_returns_at_once_on_success(self):
        sleeps = []
        self.assertEqual(res.retry_call(lambda: 7, "x", sleep=sleeps.append), 7)
        self.assertEqual(sleeps, [])

    def test_retries_a_rate_limit_with_backoff_and_jitter(self):
        calls = []
        sleeps = []

        def flaky():
            calls.append(1)
            if len(calls) < 3:
                raise Exception("429 too many requests")
            return "ok"

        result = res.retry_call(flaky, "x", max_attempts=3, initial_delay=2.0, multiplier=2.0, jitter=0.5,
                                sleep=sleeps.append, rng=lambda: 1.0)
        self.assertEqual(result, "ok")
        self.assertEqual(sleeps, [3.0, 6.0])

    def test_gives_up_after_the_last_attempt(self):
        calls = []

        def always_limited():
            calls.append(1)
            raise Exception("rate limit")

        with self.assertRaises(Exception):
            res.retry_call(always_limited, "x", max_attempts=2, sleep=lambda s: None)
        self.assertEqual(len(calls), 2)

    def test_other_errors_are_not_retried(self):
        calls = []

        def invalid():
            calls.append(1)
            raise YFTickerMissingError("no data found")

        with self.assertRaises(YFTickerMissingError):
            res.retry_call(invalid, "x", sleep=lambda s: None)
        self.assertEqual(len(calls), 1)

    def test_default_is_two_attempts(self):
        self.assertEqual(res.RETRY_MAX_ATTEMPTS, 2)

    def test_transient_keywords_include_service_unavailable(self):
        calls = []

        def unavailable():
            calls.append(1)
            if len(calls) == 1:
                raise Exception("503 Service Unavailable")
            return "ok"

        self.assertEqual(
            res.retry_call(unavailable, "x", keywords=res.TRANSIENT_KEYWORDS, sleep=lambda s: None), "ok"
        )


class ConversionRateTest(unittest.TestCase):
    def setUp(self):
        res._conversion_rates.clear()

    def response(self, status=200, rates=None):
        r = mock.Mock()
        r.status_code = status
        r.json.return_value = {"rates": rates if rates is not None else {"BRL": 5.0}}
        return r

    def test_same_currency_needs_no_call(self):
        with mock.patch.object(res.requests, "get") as get:
            self.assertEqual(res.get_conversion_rate("BRL"), 1.0)
            get.assert_not_called()

    def test_rate_is_read_once_per_currency(self):
        with mock.patch.object(res.requests, "get", return_value=self.response()) as get:
            self.assertEqual(res.get_conversion_rate("USD"), 5.0)
            self.assertEqual(res.get_conversion_rate("USD"), 5.0)
            self.assertEqual(get.call_count, 1)

    def test_usx_is_a_hundredth_of_usd(self):
        with mock.patch.object(res.requests, "get", return_value=self.response()):
            self.assertAlmostEqual(res.get_conversion_rate("USX"), 0.05)

    def test_failure_raises_instead_of_guessing(self):
        with mock.patch.object(res.requests, "get", return_value=self.response(status=500)):
            with mock.patch.object(res.time, "sleep"):
                with self.assertRaises(Exception):
                    res.get_conversion_rate("EUR")

    def test_missing_currency_raises(self):
        with mock.patch.object(res.requests, "get", return_value=self.response(rates={"USD": 1})):
            with self.assertRaises(ValueError):
                res.get_conversion_rate("EUR")


class LookupCurrencyTest(unittest.TestCase):
    def yf_with(self, info, fast_info=None):
        ticker = mock.Mock()
        ticker.info = info
        ticker.fast_info = fast_info if fast_info is not None else {}
        yf = mock.Mock()
        yf.Ticker.return_value = ticker
        return yf

    def test_reads_currency_and_website_from_info(self):
        yf = self.yf_with({"currency": "USD", "website": "https://a.example"})
        self.assertEqual(res.lookup_currency_and_website("AAA", yf), ("USD", "https://a.example"))

    def test_falls_back_to_fast_info_for_the_currency(self):
        yf = self.yf_with({}, fast_info={"currency": "BRL"})
        self.assertEqual(res.lookup_currency_and_website("AAA", yf), ("BRL", None))

    def test_unknown_currency_raises_and_is_never_assumed(self):
        yf = self.yf_with({})
        with self.assertRaises(ValueError):
            res.lookup_currency_and_website("AAA", yf)

    def test_rate_limit_in_fast_info_is_not_swallowed(self):
        class Limited(dict):
            def __getitem__(self, key):
                raise YFRateLimitError("429")

        yf = self.yf_with({}, fast_info=Limited())
        with self.assertRaises(YFRateLimitError):
            res.lookup_currency_and_website("AAA", yf)


if __name__ == "__main__":
    unittest.main()
