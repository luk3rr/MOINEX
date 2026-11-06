# Filename: yf_resilience.py
# Created on: November 6, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
Shared helpers of the scripts that talk to Yahoo Finance (and the other web APIs the app reads).

It is not a script: the app copies it next to the script it runs. It gathers what the scripts used to
copy from each other: retry with backoff and jitter, the classification of errors (so the Kotlin side
can open a cooldown on a rate limit), the DNS workaround for Windows and the currency conversion.
"""

import contextlib
import logging
import random
import sys
import time

import requests

KIND_RATE_LIMIT = "rate_limit"
KIND_INVALID_SYMBOL = "invalid_symbol"
KIND_CURRENCY_UNKNOWN = "currency_unknown"
KIND_ERROR = "error"

RATE_LIMIT_KEYWORDS = ("429", "rate limit", "ratelimit", "too many requests")
TRANSIENT_KEYWORDS = RATE_LIMIT_KEYWORDS + ("503", "service unavailable")
INVALID_SYMBOL_KEYWORDS = (
    "no data found",
    "no price data",
    "possibly delisted",
    "symbol may be delisted",
    "tickermissing",
    "pricesmissing",
    "not found",
)

RETRY_MAX_ATTEMPTS = 2
RETRY_INITIAL_DELAY = 2.0
RETRY_MULTIPLIER = 2.0
RETRY_JITTER = 0.25

DEFAULT_CURRENCY = "BRL"
EXCHANGE_API_URL_BASE = "https://api.exchangerate-api.com/v4/latest"
TIMEOUT_SECONDS = 20
WINDOWS_DNS_SERVERS = "1.1.1.1,8.8.8.8"

_conversion_rates = {}


def use_explicit_dns_on_windows():
    """
    On Windows the resolver bundled with curl_cffi (c-ares) can take ~20s per
    lookup while the system resolver answers in milliseconds. Giving libcurl
    explicit DNS servers avoids the stall. No-op on other platforms.
    """
    if sys.platform != "win32":
        return
    try:
        from curl_cffi import CurlOpt
        from curl_cffi.requests import Session
    except ImportError:
        return

    original_init = Session.__init__

    def init_with_dns(self, *args, **kwargs):
        options = dict(kwargs.get("curl_options") or {})
        options.setdefault(CurlOpt.DNS_SERVERS, WINDOWS_DNS_SERVERS)
        kwargs["curl_options"] = options
        original_init(self, *args, **kwargs)

    Session.__init__ = init_with_dns


class _MessageCollector(logging.Handler):
    def __init__(self):
        super().__init__(level=logging.WARNING)
        self.messages = []

    def emit(self, record):
        self.messages.append(record.getMessage())


@contextlib.contextmanager
def capture_yfinance_errors():
    """
    yf.download() does not raise when a symbol fails (including on a rate limit): it logs the error on the
    "yfinance" logger and returns what it could read. This collects those messages while the block runs, so
    the caller can tell a rate limit from a symbol that simply has no data.
    """
    handler = _MessageCollector()
    logger = logging.getLogger("yfinance")
    logger.addHandler(handler)
    try:
        yield handler.messages
    finally:
        logger.removeHandler(handler)


def messages_about(symbol, messages):
    """The captured messages that mention the symbol."""
    return [m for m in messages if symbol in m]


def matches_keywords(error, keywords):
    """True when the error text, or the name of its type, contains one of the keywords."""
    text = f"{type(error).__name__} {error}".lower() if isinstance(error, BaseException) else str(error).lower()
    return any(keyword in text for keyword in keywords)


def is_rate_limit(error):
    """True for yfinance's YFRateLimitError and for any error that says 429 / rate limit / too many requests."""
    return matches_keywords(error, RATE_LIMIT_KEYWORDS)


def classify_error(error):
    """
    Names the kind of an error: rate_limit, invalid_symbol or error. The Kotlin side opens a cooldown only
    for rate_limit, so an unknown or delisted symbol never pauses the other calls.
    """
    if is_rate_limit(error):
        return KIND_RATE_LIMIT
    if matches_keywords(error, INVALID_SYMBOL_KEYWORDS):
        return KIND_INVALID_SYMBOL
    return KIND_ERROR


def error_payload(error, kind=None, **extra):
    """The JSON object that describes a failure: {"error": text, "error_kind": kind, ...}."""
    payload = {"error": str(error), "error_kind": kind or classify_error(error)}
    payload.update(extra)
    return payload


def retry_call(
    fn,
    label,
    max_attempts=RETRY_MAX_ATTEMPTS,
    initial_delay=RETRY_INITIAL_DELAY,
    multiplier=RETRY_MULTIPLIER,
    jitter=RETRY_JITTER,
    keywords=RATE_LIMIT_KEYWORDS,
    sleep=None,
    rng=None,
):
    """
    Calls fn(), retrying only when the error matches one of the keywords (a rate limit by default),
    with exponential backoff and jitter. Any other error is raised at once. Writes retry status to stderr.
    """
    delay = initial_delay
    for attempt in range(1, max_attempts + 1):
        try:
            return fn()
        except Exception as e:
            if not matches_keywords(e, keywords) or attempt == max_attempts:
                raise
            wait = delay * (1 + jitter * (rng or random.random)())
            print(
                f"[RETRY] {label} attempt {attempt + 1}/{max_attempts} after {wait:.1f}s delay: {e}",
                file=sys.stderr,
            )
            (sleep or time.sleep)(wait)
            delay *= multiplier


def get_conversion_rate(from_currency, to_currency=DEFAULT_CURRENCY):
    """
    Rate that converts one unit of from_currency into to_currency, read once per run per currency.
    USX (cents of dollar) is handled through USD. Raises when the rate cannot be read: the caller must
    not guess one, since a wrong rate would store a wrong price.
    """
    if from_currency == to_currency:
        return 1.0
    if from_currency == "USX":
        return get_conversion_rate("USD", to_currency) / 100
    if to_currency == "USX":
        return get_conversion_rate(from_currency, "USD") * 100

    key = (from_currency, to_currency)
    if key in _conversion_rates:
        return _conversion_rates[key]

    def fetch():
        response = requests.get(f"{EXCHANGE_API_URL_BASE}/{from_currency}", timeout=TIMEOUT_SECONDS)
        if response.status_code != 200:
            raise Exception(f"Failed to get the conversion rate for {from_currency} (HTTP {response.status_code})")
        rates = response.json()["rates"]
        if to_currency not in rates:
            raise ValueError(f"Currency {to_currency} or {from_currency} not found in the response")
        return rates[to_currency]

    rate = retry_call(fetch, f"{from_currency}->{to_currency}", keywords=TRANSIENT_KEYWORDS)
    _conversion_rates[key] = rate
    return rate


def lookup_currency_and_website(symbol, yf):
    """
    Reads the currency and the website of a symbol from yfinance info, the one heavy call the app makes once
    per ticker. When info has no currency it asks the light fast_info. Raises when no currency is found:
    a price is never converted with an assumed currency. Returns (currency, website_or_None).
    """
    ticker = yf.Ticker(symbol)
    info = ticker.info
    currency = info.get("currency")

    if not currency:
        try:
            currency = ticker.fast_info["currency"]
        except Exception as e:
            if is_rate_limit(e):
                raise

    if not currency:
        raise ValueError(f"Currency of {symbol} is unknown")
    return currency, info.get("website")
