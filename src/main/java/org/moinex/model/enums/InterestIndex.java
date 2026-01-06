/*
 * Filename: InterestIndex.java
 * Created on: January 14, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.enums;

/**
 * ENUM that represents the index of interest of a bond
 */
public enum InterestIndex {
    // Brazilian Indices
    CDI, // Certificado de Depósito Interbancário
    SELIC, // Sistema Especial de Liquidação e de Custódia
    IPCA, // Índice Nacional de Preços ao Consumidor Amplo

    // International Indices
    LIBOR, // London Interbank Offered Rate
    SOFR, // Secured Overnight Financing Rate
    OTHER // Other indices aren't specified
}
