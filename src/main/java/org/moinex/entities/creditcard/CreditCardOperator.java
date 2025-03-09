/*
 * Filename: CreditCardOperator.java
 * Created on: September 17, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.creditcard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a credit card operator
 * A credit card operator is a company that issues credit cards
 */
@Entity
@Table(name = "credit_card_operator")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardOperator
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "icon", nullable = true, length = 30)
    private String icon;

    /**
     * Constructor for testing purposes
     */
    public CreditCardOperator(Long id, String name)
    {
        this.id   = id;
        this.name = name;
    }
}
