/*
 * Filename: SankeyChart.kt
 * Created on: April 3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.chart

import javafx.geometry.VPos
import javafx.scene.control.Tooltip
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.ClosePath
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import org.moinex.common.constant.Styles
import org.moinex.common.util.UIUtils
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory

data class SankeyNodeData(
    val name: String,
    val value: Double = 0.0,
    val id: String = name,
)

data class SankeyLinkData(
    val source: String,
    val target: String,
    val value: Double,
)

data class NodeLayout(
    val id: String,
    val name: String,
    val color: Color,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    var outOffset: Double = 0.0,
    var inOffset: Double = 0.0,
)

class SankeyChart : Pane() {
    var preferencesService: PreferencesService? = null

    companion object {
        private val logger = LoggerFactory.getLogger(SankeyChart::class.java)
        private const val PADDING = 10.0
        private const val NODE_WIDTH = 10.0
        private const val NODE_GAP = 7.0
        private const val FLOW_OPACITY = 0.45
        private const val HIGHLIGHT_OPACITY = 0.75
        private const val FONT_SIZE = 11.0
        private val TEXT_COLOR = Color.BLACK
        private const val RIGHT_ARROW = "\u2794"
        private const val LEFT_CONTROL_PROPORTION = 0.4
        private const val RIGHT_CONTROL_PROPORTION = 0.6
        private const val LABEL_PADDING = 5.0
    }

    private val colorPalette =
        listOf(
            Color.web("#FF6B6B"),
            Color.web("#4ECDC4"),
            Color.web("#45B7D1"),
            Color.web("#FFA07A"),
            Color.web("#98D8C8"),
            Color.web("#F7DC6F"),
            Color.web("#BB8FCE"),
            Color.web("#85C1E2"),
            Color.web("#F06292"),
            Color.web("#AED581"),
        )

    private var currentNodes: List<SankeyNodeData> = emptyList()
    private var currentLinks: List<SankeyLinkData> = emptyList()

    init {
        widthProperty().addListener { _, _, _ -> render() }
        heightProperty().addListener { _, _, _ -> render() }
    }

    fun updateData(
        nodes: List<SankeyNodeData>,
        links: List<SankeyLinkData>,
    ) {
        currentNodes = nodes
        currentLinks = links
        render()
    }

    private fun render() {
        children.clear()

        if (currentNodes.isEmpty() || currentLinks.isEmpty()) return
        if (width <= 0 || height <= 0) return

        val acyclicLinks = breakCycles(currentNodes.map { it.id }, currentLinks)
        if (acyclicLinks.isEmpty()) return

        val levels = computeLevels(acyclicLinks)
        val maxLevel = levels.values.maxOrNull() ?: 0

        val nodeColorMap = mutableMapOf<String, Color>()
        currentNodes.forEachIndexed { idx, node ->
            nodeColorMap[node.id] = colorPalette[idx % colorPalette.size]
        }

        val nodeFlows = computeNodeFlows(acyclicLinks)

        val initialNodesPerLevel =
            currentNodes
                .groupBy { levels[it.id] ?: 0 }
                .toSortedMap()

        val optimizedNodesPerLevel = optimizeNodeOrder(initialNodesPerLevel, acyclicLinks)

        val maxTotalFlow =
            optimizedNodesPerLevel.values.maxOfOrNull { levelNodes ->
                levelNodes.sumOf { nodeFlows[it.id] ?: 0.0 }
            } ?: return

        if (maxTotalFlow <= 0) return

        // Measure label widths to reserve space
        val labelFont = Font.font(FONT_SIZE)
        val maxLeftLabelWidth =
            optimizedNodesPerLevel
                .filterKeys { it < maxLevel }
                .values
                .flatten()
                .maxOfOrNull { measureTextWidth(it.name, labelFont) } ?: 0.0
        val maxRightLabelWidth =
            (optimizedNodesPerLevel[maxLevel] ?: emptyList())
                .maxOfOrNull { measureTextWidth(it.name, labelFont) } ?: 0.0

        val leftPadding = PADDING + maxLeftLabelWidth + 8.0
        val rightPadding = PADDING + maxRightLabelWidth + 8.0
        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - 2 * PADDING

        if (drawWidth <= 0 || drawHeight <= 0) return

        val horizontalGap = if (maxLevel > 0) (drawWidth - NODE_WIDTH) / maxLevel else 0.0
        val maxNodesInLevel = optimizedNodesPerLevel.values.maxOf { it.size }
        val maxGap = (maxNodesInLevel - 1) * NODE_GAP
        val scaleY = (drawHeight - maxGap) / maxTotalFlow

        val layouts =
            layoutNodes(
                optimizedNodesPerLevel,
                nodeFlows,
                nodeColorMap,
                leftPadding,
                horizontalGap,
                scaleY,
                drawHeight,
            )

        val sortedLinksForDrawing =
            acyclicLinks.sortedWith(
                compareBy<SankeyLinkData> { layouts[it.source]?.y ?: 0.0 }
                    .thenBy { layouts[it.target]?.y ?: 0.0 },
            )
        drawFlows(sortedLinksForDrawing, layouts, scaleY)

        drawNodes(layouts)

        drawLabels(layouts, levels, maxLevel, labelFont)
    }

    private fun layoutNodes(
        nodesPerLevel: Map<Int, List<SankeyNodeData>>,
        nodeFlows: Map<String, Double>,
        nodeColorMap: Map<String, Color>,
        leftPadding: Double,
        horizontalGap: Double,
        scaleY: Double,
        drawHeight: Double,
    ): Map<String, NodeLayout> {
        val layouts = mutableMapOf<String, NodeLayout>()

        for ((level, levelNodes) in nodesPerLevel) {
            val nodeCount = levelNodes.size
            val totalGap = (nodeCount - 1) * NODE_GAP
            val totalLevelFlow = levelNodes.sumOf { nodeFlows[it.id] ?: 0.0 }
            val levelHeight = totalLevelFlow * scaleY + totalGap
            val startY = PADDING + (drawHeight - levelHeight) / 2.0
            val x = leftPadding + horizontalGap * level

            var currentY = startY
            for (node in levelNodes) {
                val h = (nodeFlows[node.id] ?: 0.0) * scaleY
                if (h <= 0) continue

                layouts[node.id] =
                    NodeLayout(
                        id = node.id,
                        name = node.name,
                        color = nodeColorMap[node.id] ?: colorPalette[0],
                        x = x,
                        y = currentY,
                        w = NODE_WIDTH,
                        h = h,
                    )
                currentY += h + NODE_GAP
            }
        }

        return layouts
    }

    private fun drawFlows(
        sortedLinks: List<SankeyLinkData>,
        layouts: Map<String, NodeLayout>,
        scaleY: Double,
    ) {
        for (link in sortedLinks) {
            val src = layouts[link.source] ?: continue
            val tgt = layouts[link.target] ?: continue
            val thickness = link.value * scaleY
            if (thickness <= 0) continue

            val srcY = src.y + src.outOffset
            val tgtY = tgt.y + tgt.inOffset
            src.outOffset += thickness
            tgt.inOffset += thickness

            val x1 = src.x + src.w
            val x2 = tgt.x
            val cx1 = x1 + (x2 - x1) * LEFT_CONTROL_PROPORTION
            val cx2 = x1 + (x2 - x1) * RIGHT_CONTROL_PROPORTION

            val flowPath =
                Path(
                    MoveTo(x1, srcY),
                    CubicCurveTo(cx1, srcY, cx2, tgtY, x2, tgtY),
                    LineTo(x2, tgtY + thickness),
                    CubicCurveTo(cx2, tgtY + thickness, cx1, srcY + thickness, x1, srcY + thickness),
                    ClosePath(),
                )

            val gradient = createGradient(src.color, tgt.color, FLOW_OPACITY)
            val highlightGradient = createGradient(src.color, tgt.color, HIGHLIGHT_OPACITY)

            flowPath.fill = gradient
            flowPath.stroke = null

            val tooltipText = "${src.name} $RIGHT_ARROW ${tgt.name}: ${UIUtils.formatCurrency(link.value)}"
            val tooltip =
                Tooltip(tooltipText).apply {
                    styleClass.add(Styles.TOOLTIP_STYLE)
                }

            flowPath.setOnMousePressed { e ->
                flowPath.fill = highlightGradient
                tooltip.show(flowPath, e.screenX + 10, e.screenY + 10)
            }

            flowPath.setOnMouseReleased {
                flowPath.fill = gradient
                tooltip.hide()
            }

            children.add(flowPath)
        }
    }

    private fun drawNodes(layouts: Map<String, NodeLayout>) {
        for ((_, layout) in layouts) {
            val rect =
                Rectangle(layout.x, layout.y, layout.w, layout.h).apply {
                    fill = layout.color
                    arcWidth = 3.0
                    arcHeight = 3.0
                }
            children.add(rect)
        }
    }

    private fun drawLabels(
        layouts: Map<String, NodeLayout>,
        levels: Map<String, Int>,
        maxLevel: Int,
        font: Font,
    ) {
        for ((_, layout) in layouts) {
            val level = levels[layout.id] ?: 0
            val text =
                Text(layout.name).apply {
                    this.font = font
                    fill = TEXT_COLOR
                    textOrigin = VPos.CENTER
                }

            if (level == maxLevel) {
                text.x = layout.x + layout.w + LABEL_PADDING
            } else {
                text.x = layout.x - measureTextWidth(layout.name, font) - LABEL_PADDING
            }
            text.y = layout.y + layout.h / 2.0

            children.add(text)
        }
    }

    private fun createGradient(
        srcColor: Color,
        tgtColor: Color,
        opacity: Double,
    ): LinearGradient =
        LinearGradient(
            0.0,
            0.0,
            1.0,
            0.0,
            true,
            CycleMethod.NO_CYCLE,
            Stop(0.0, srcColor.deriveColor(0.0, 1.0, 1.0, opacity)),
            Stop(1.0, tgtColor.deriveColor(0.0, 1.0, 1.0, opacity)),
        )

    private fun computeLevels(acyclicLinks: List<SankeyLinkData>): Map<String, Int> {
        val outgoingMap = currentNodes.associate { it.id to mutableSetOf<String>() }
        val incomingMap = currentNodes.associate { it.id to mutableSetOf<String>() }

        acyclicLinks.forEach { link ->
            outgoingMap[link.source]?.add(link.target)
            incomingMap[link.target]?.add(link.source)
        }

        val levels = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        currentNodes.forEach { node ->
            if (incomingMap[node.id].isNullOrEmpty()) {
                levels[node.id] = 0
                queue.add(node.id)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentLevel = levels[current]
            outgoingMap[current]?.forEach { target ->
                val newLevel = currentLevel!! + 1
                if ((levels[target] ?: -1) < newLevel) {
                    levels[target] = newLevel
                    queue.add(target)
                }
            }
        }

        return levels
    }

    private fun computeNodeFlows(acyclicLinks: List<SankeyLinkData>): Map<String, Double> {
        val nodeFlows = mutableMapOf<String, Double>()
        currentNodes.forEach { node ->
            val incoming = acyclicLinks.filter { it.target == node.id }.sumOf { it.value }
            val outgoing = acyclicLinks.filter { it.source == node.id }.sumOf { it.value }
            nodeFlows[node.id] = maxOf(incoming, outgoing)
        }
        return nodeFlows
    }

    private fun measureTextWidth(
        text: String,
        font: Font,
    ): Double {
        val helper = Text(text)
        helper.font = font
        return helper.boundsInLocal.width
    }

    /**
     * Removes back-edges to break cycles and ensure the graph is a Directed Acyclic Graph (DAG).
     *
     * A DAG is strictly required to compute the hierarchical levels of a Sankey chart correctly.
     * This implementation uses a Depth-First Search (DFS) with a tri-color marking algorithm:
     * - WHITE (0): Unvisited node.
     * - GRAY (1): Node is currently in the traversal path.
     * - BLACK (2): Node and all its descendants have been fully processed.
     *
     * If an edge points to a GRAY node, it is identified as a back-edge (forming a cycle) and is removed.
     *
     * @param nodeIds A list containing the unique identifiers of all nodes in the chart.
     * @param links The initial list of connections (flows) between nodes.
     * @return A new list of links with all cycle-causing back-edges removed.
     */
    private fun breakCycles(
        nodeIds: List<String>,
        links: List<SankeyLinkData>,
    ): List<SankeyLinkData> {
        val result = links.toMutableList()
        val color = nodeIds.associateWith { 0 }.toMutableMap()

        fun dfs(node: String) {
            color[node] = 1
            result.filter { it.source == node }.toList().forEach { link ->
                when (color[link.target]) {
                    1 -> {
                        logger.warn(
                            "Cycle detected: {} -> {}. Removing link.",
                            link.source,
                            link.target,
                        )
                        result.remove(link)
                    }
                    0 -> dfs(link.target)
                }
            }
            color[node] = 2
        }

        nodeIds.forEach { if (color[it] == 0) dfs(it) }
        return result
    }

    /**
     * Optimizes the vertical order of nodes within each level to minimize edge crossings.
     *
     * This function applies the Barycenter Heuristic using a left-to-right sweep.
     * For each node in the current level, its ideal vertical position is calculated as the
     * weighted average of the vertical positions of its incoming source nodes from the previous level.
     *
     * @param nodesPerLevel A map containing nodes grouped by their level (column index).
     * @param links The list of links (edges) representing the flow between nodes.
     * @return A new map with the nodes vertically sorted within each level.
     */
    private fun optimizeNodeOrder(
        nodesPerLevel: Map<Int, List<SankeyNodeData>>,
        links: List<SankeyLinkData>,
    ): Map<Int, List<SankeyNodeData>> {
        val optimizedNodes = nodesPerLevel.toMutableMap()
        val maxLevel = nodesPerLevel.keys.maxOrNull() ?: return nodesPerLevel

        for (level in 1..maxLevel) {
            val prevLevelNodes = optimizedNodes[level - 1] ?: continue
            val currentLevelNodes = optimizedNodes[level] ?: continue

            val prevPositions = prevLevelNodes.mapIndexed { index, node -> node.id to index }.toMap()

            val sortedCurrent =
                currentLevelNodes.sortedBy { node ->
                    val incomingLinks = links.filter { it.target == node.id && prevPositions.containsKey(it.source) }

                    if (incomingLinks.isEmpty()) {
                        0.0
                    } else {
                        val weightedSum = incomingLinks.sumOf { prevPositions[it.source]!! * it.value }
                        val totalVolume = incomingLinks.sumOf { it.value }
                        weightedSum / totalVolume
                    }
                }
            optimizedNodes[level] = sortedCurrent
        }

        return optimizedNodes
    }
}
