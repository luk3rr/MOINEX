#!/usr/bin/env python3

# Filename: bacen_api_fetcher.py
# Created on: January 15, 2025
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import requests
import pandas as pd

BACEN_API_BASE_URL = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.{}/dados?formato=json&dataInicial={}&dataFinal={}"

SELIC_TARGET_SERIES = 432
IPCA_SERIES = 433
CDI_SERIES = 12


def get_selic_target(start_date: str, end_date: str) -> pd.DataFrame:
    """
    Obtém a Taxa Selic Meta a partir da API do Banco Central.

    :param start_date: Data inicial no formato DD/MM/YYYY
    :param end_date: Data final no formato DD/MM/YYYY
    :return: DataFrame com datas e valores da Selic Meta
    """
    url = BACEN_API_BASE_URL.format(SELIC_TARGET_SERIES, start_date, end_date)

    response = requests.get(url)
    if response.status_code == 200:
        data = response.json()
        df = pd.DataFrame(data)
        df["data"] = pd.to_datetime(df["data"], dayfirst=True)
        df["valor"] = df["valor"].astype(float)
        return df
    else:
        raise Exception(
            f"Erro ao acessar a API do Banco Central: {response.status_code}"
        )


def get_ipca(start_date: str, end_date: str):
    """
    Obtém o IPCA a partir da API do Banco Central.

    :param start_date: Data inicial no formato DD/MM/YYYY
    :param end_date: Data final no formato DD/MM/YYYY
    :return: DataFrame com datas e valores do IPCA
    """
    url = BACEN_API_BASE_URL.format(IPCA_SERIES, start_date, end_date)

    response = requests.get(url)
    if response.status_code == 200:
        data = response.json()
        df = pd.DataFrame(data)
        df["data"] = pd.to_datetime(df["data"], dayfirst=True)
        df["valor"] = df["valor"].astype(float)
        return df
    else:
        raise Exception(
            f"Erro ao acessar a API do Banco Central: {response.status_code}"
        )


def get_cdi(start_date: str, end_date: str):
    """
    Obtém o CDI a partir da API do Banco Central.

    :param start_date: Data inicial no formato DD/MM/YYYY
    :param end_date: Data final no formato DD/MM/YYYY
    :return: DataFrame com datas e valores do CDI
    """
    url = BACEN_API_BASE_URL.format(CDI_SERIES, start_date, end_date)

    response = requests.get(url)
    if response.status_code == 200:
        data = response.json()
        df = pd.DataFrame(data)
        df["data"] = pd.to_datetime(df["data"], dayfirst=True)
        df["valor"] = df["valor"].astype(float)
        return df
    else:
        raise Exception(
            f"Erro ao acessar a API do Banco Central: {response.status_code}"
        )


def main():
    start_date = "01/12/2024"
    end_date = "01/01/2025"
    selic_data = get_selic_target(start_date, end_date)
    ipca_data = get_ipca(start_date, end_date)
    cdi_data = get_cdi(start_date, end_date)

    print("Taxa Selic Meta:")
    print(selic_data)

    print("\nIPCA:")
    print(ipca_data)

    print("\nCDI:")
    print(cdi_data)


if __name__ == "__main__":
    main()
