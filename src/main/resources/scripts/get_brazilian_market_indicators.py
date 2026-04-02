#!/usr/bin/env python3

# Filename: get_brazilian_market_indicators.py
# Created on: January 16, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import time
import requests
from datetime import datetime, timedelta

BACEN_API_SERIES_URL = (
    "https://api.bcb.gov.br/dados/serie/bcdata.sgs.{}/dados?formato=json"
)

BACEN_API_SERIES_WITH_FILTER_URL = BACEN_API_SERIES_URL + "&dataInicial={}&dataFinal={}"

DEFAULT_CURRENCY = "BRL"

SELIC_TARGET_SERIES = 432
IPCA_SERIES = 433
CDI_SERIES = 12

TIMEOUT_SECONDS = 20

RETRY_MAX_ATTEMPTS = 3
RETRY_INITIAL_DELAY = 1.0
RETRY_MULTIPLIER = 1.5
RETRY_KEYWORDS = ("429", "rate limit", "too many requests", "503", "service unavailable")


def retry_call(fn, label):
    """
    Calls fn(), retrying on transient errors with exponential backoff.
    Writes retry status to stderr. Returns the result or raises on exhaustion.
    """
    delay = RETRY_INITIAL_DELAY
    for attempt in range(1, RETRY_MAX_ATTEMPTS + 1):
        try:
            return fn()
        except Exception as e:
            error_str = str(e).lower()
            is_transient = any(k in error_str for k in RETRY_KEYWORDS)
            if not is_transient or attempt == RETRY_MAX_ATTEMPTS:
                raise
            print(
                f"[RETRY] {label} attempt {attempt + 1}/{RETRY_MAX_ATTEMPTS} "
                f"after {delay:.0f}s delay: {e}",
                file=sys.stderr,
            )
            time.sleep(delay)
            delay *= RETRY_MULTIPLIER


def download_data(url: str) -> dict:
    """
    Faz o download de dados de uma URL

    :param url: URL para download
    :return: Dados obtidos
    """
    response = requests.get(url, timeout=TIMEOUT_SECONDS)

    if response.status_code == 200:
        data = response.json()
        return data
    else:
        raise Exception(
            f"Erro ao acessar a URL {url}. Código de erro: {response.status_code}"
        )


def get_ipca_12_months(ipca_series: list) -> dict:
    """
    Calcula o IPCA acumulado nos últimos 12 meses.

    :param ipca_series: Lista de valores do IPCA
    :return: IPCA acumulado em 12 meses (percentual)
    """

    if len(ipca_series) < 12:
        raise ValueError("A lista de valores do IPCA deve conter pelo menos 12 meses")

    last_12_months = ipca_series[-12:]

    montly_index = 1
    accumulated_index = 1

    for ipca in last_12_months:
        montly_index = 1 + (float(ipca["valor"]) / 100)
        accumulated_index *= montly_index

    ipca_acumulado_percentual = (accumulated_index - 1) * 100
    return {"valor": ipca_acumulado_percentual}


def main():
    data = {}

    # Two year of data is enough for our purposes.
    # Ensure that there is enough data to calculate the IPCA accumulated
    today = datetime.today().strftime("%d/%m/%Y")
    two_year_ago = (datetime.today() - timedelta(days=2 * 365)).strftime("%d/%m/%Y")

    selic_url = BACEN_API_SERIES_WITH_FILTER_URL.format(
        SELIC_TARGET_SERIES, two_year_ago, today
    )
    selic_series = retry_call(lambda: download_data(selic_url), "SELIC")

    ipca_url = BACEN_API_SERIES_WITH_FILTER_URL.format(IPCA_SERIES, two_year_ago, today)
    ipca_series = retry_call(lambda: download_data(ipca_url), "IPCA")

    data["selic_target"] = selic_series[-1]
    data["ipca_last_month"] = ipca_series[-1]
    data["ipca_12_months"] = get_ipca_12_months(ipca_series[-12:])

    print(data)


if __name__ == "__main__":
    main()
