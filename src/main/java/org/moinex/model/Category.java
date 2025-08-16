/*
 * Filename: Category.java
 * Created on: March 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model;

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
 * Represents a category of expenses and incomes
 */
@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Builder.Default
    @Column(name = "archived", nullable = false)
    private boolean isArchived = false; // Default value is false

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id != null && id.equals(category.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}
