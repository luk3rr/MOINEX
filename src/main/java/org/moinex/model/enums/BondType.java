/*
 * Filename: BondType.java
 * Created on: January 14, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.enums;

/**
 * ENUM that represents the type of bond
 */
public enum BondType {
    // Brazilian Bonds
    CDB, // Certificado de Depósito Bancário
    LCI, // Letra de Crédito Imobiliário
    LCA, // Letra de Crédito do Agronegócio
    TREASURY_PREFIXED, // Tesouro Direto - Prefixado
    TREASURY_POSTFIXED, // Tesouro Direto - Pós-Fixado

    // International or generic Bonds
    INTERNATIONAL, // International Bonds (generic placeholder)
    OTHER // Other types of bonds
}
