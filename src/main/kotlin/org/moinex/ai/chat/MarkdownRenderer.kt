/*
 * Filename: MarkdownRenderer.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.HtmlRenderer

internal object MarkdownRenderer {
    private val extensions = listOf(TablesExtension.create())

    private val parser: Parser = Parser.builder().extensions(extensions).build()

    private val htmlRenderer: HtmlRenderer =
        HtmlRenderer
            .builder()
            .extensions(extensions)
            .escapeHtml(true)
            .attributeProviderFactory {
                AttributeProvider { node, _, attributes -> sanitizeLinkHref(node, attributes) }
            }.build()

    fun toHtmlFragment(markdown: String): String {
        val document: Node = parser.parse(markdown)
        return htmlRenderer.render(document)
    }

    private fun sanitizeLinkHref(
        node: Node,
        attributes: MutableMap<String, String>,
    ) {
        if (node !is Link) return

        val href = attributes["href"].orEmpty()
        val isSafeHttpLink =
            href.startsWith("http://", ignoreCase = true) || href.startsWith("https://", ignoreCase = true)
        if (!isSafeHttpLink) {
            attributes.remove("href")
        }
    }
}
