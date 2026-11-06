#!/usr/bin/env python3

# Filename: get_brazilian_market_indicators.py
# Created on: January 16, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import os
import sys
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

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import yf_resilience as res


def retry_call(fn, label):
    """Retries transient BACEN errors (429, 503) with the shared backoff."""
    return res.retry_call(
        fn, label, max_attempts=3, initial_delay=1.0, multiplier=1.5, keywords=res.TRANSIENT_KEYWORDS
    )


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
