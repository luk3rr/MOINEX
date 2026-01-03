/*
 * Filename: GoalAssetAllocationRepository.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.goal;

import java.util.List;
import java.util.Optional;
import org.moinex.model.goal.Goal;
import org.moinex.model.goal.GoalAssetAllocation;
import org.moinex.util.enums.GoalAssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalAssetAllocationRepository extends JpaRepository<GoalAssetAllocation, Integer> {

    /**
     * Busca todas as alocações de uma goal
     *
     * @param goal Goal para buscar alocações
     * @return Lista de alocações da goal
     */
    List<GoalAssetAllocation> findByGoal(Goal goal);

    /**
     * Busca todas as alocações de um ativo específico
     *
     * @param assetType Tipo do ativo (BOND ou TICKER)
     * @param assetId ID do ativo
     * @return Lista de alocações do ativo
     */
    @Query("SELECT gaa FROM GoalAssetAllocation gaa " +
           "WHERE gaa.assetType = :assetType AND gaa.assetId = :assetId")
    List<GoalAssetAllocation> findByAssetTypeAndAssetId(
        @Param("assetType") GoalAssetType assetType,
        @Param("assetId") Integer assetId
    );

    /**
     * Busca alocação específica de um ativo em uma goal
     *
     * @param goalId ID da goal
     * @param assetType Tipo do ativo
     * @param assetId ID do ativo
     * @return Alocação encontrada ou null
     */
    @Query("SELECT gaa FROM GoalAssetAllocation gaa " +
           "WHERE gaa.goal.id = :goalId " +
           "AND gaa.assetType = :assetType " +
           "AND gaa.assetId = :assetId")
    Optional<GoalAssetAllocation> findByGoalAndAsset(
        @Param("goalId") Integer goalId,
        @Param("assetType") GoalAssetType assetType,
        @Param("assetId") Integer assetId
    );

    /**
     * Conta quantas goals estão vinculadas a um ativo
     *
     * @param assetType Tipo do ativo
     * @param assetId ID do ativo
     * @return Número de goals vinculadas
     */
    @Query("SELECT COUNT(DISTINCT gaa.goal) FROM GoalAssetAllocation gaa " +
           "WHERE gaa.assetType = :assetType AND gaa.assetId = :assetId")
    Long countGoalsByAsset(
        @Param("assetType") GoalAssetType assetType,
        @Param("assetId") Integer assetId
    );

    /**
     * Deleta todas as alocações de uma goal
     *
     * @param goal Goal para deletar alocações
     */
    void deleteByGoal(Goal goal);

    /**
     * Busca todas as alocações de uma goal por ID
     *
     * @param goalId ID da goal
     * @return Lista de alocações
     */
    @Query("SELECT gaa FROM GoalAssetAllocation gaa WHERE gaa.goal.id = :goalId")
    List<GoalAssetAllocation> findByGoalId(@Param("goalId") Integer goalId);
}
