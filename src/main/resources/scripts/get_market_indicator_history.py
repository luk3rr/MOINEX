#!/usr/bin/env python3

# Filename: get_market_indicator_history.py
# Created on: February 20, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
import time
import requests
from datetime import datetime

BACEN_API_SERIES_URL = (
    "https://api.bcb.gov.br/dados/serie/bcdata.sgs.{}/dados?formato=json"
)

BACEN_API_SERIES_WITH_FILTER_URL = BACEN_API_SERIES_URL + "&dataInicial={}&dataFinal={}"

SELIC_TARGET_SERIES = 432
IPCA_SERIES = 433
CDI_SERIES = 12

INDICATOR_MAPPING = {
    "SELIC": SELIC_TARGET_SERIES,
    "IPCA": IPCA_SERIES,
    "CDI": CDI_SERIES,
}

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


def download_data(url: str) -> list:
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


def validate_date_format(date_str: str) -> bool:
    """
    Valida se a data está no formato DD/MM/YYYY

    :param date_str: String de data
    :return: True se válida, False caso contrário
    """
    try:
        datetime.strptime(date_str, "%d/%m/%Y")
        return True
    except ValueError:
        return False


def get_indicator_history(indicator_type: str, start_date: str, end_date: str) -> dict:
    """
    Busca o histórico de um indicador específico

    :param indicator_type: Tipo de indicador (SELIC, IPCA, CDI)
    :param start_date: Data inicial no formato DD/MM/YYYY
    :param end_date: Data final no formato DD/MM/YYYY
    :return: Dicionário com histórico do indicador
    """
    if indicator_type not in INDICATOR_MAPPING:
        raise ValueError(
            f"Tipo de indicador inválido: {indicator_type}. "
            f"Valores válidos: {', '.join(INDICATOR_MAPPING.keys())}"
        )

    if not validate_date_format(start_date):
        raise ValueError(f"Data inicial inválida: {start_date}. Use formato DD/MM/YYYY")

    if not validate_date_format(end_date):
        raise ValueError(f"Data final inválida: {end_date}. Use formato DD/MM/YYYY")

    series_id = INDICATOR_MAPPING[indicator_type]
    url = BACEN_API_SERIES_WITH_FILTER_URL.format(series_id, start_date, end_date)

    history = retry_call(lambda: download_data(url), indicator_type)

    return {
        "indicator_type": indicator_type,
        "start_date": start_date,
        "end_date": end_date,
        "data": history,
        "count": len(history),
    }


def main():
    if len(sys.argv) < 4:
        raise ValueError(
            "Uso: python3 get_market_indicator_history.py <indicator_type> <start_date> <end_date>\n"
            "Exemplo: python3 get_market_indicator_history.py CDI 01/01/2024 20/02/2026\n"
            "Indicadores disponíveis: SELIC, IPCA, CDI"
        )

    indicator_type = sys.argv[1]
    start_date = sys.argv[2]
    end_date = sys.argv[3]

    result = get_indicator_history(indicator_type, start_date, end_date)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
