package ru.skillbranch.skillarticles.markdown

import android.util.Log
import ru.skillbranch.skillarticles.extensions.countDigits
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

object MarkdownParser {

    private val LINE_SEPARATOR = "\\ "

    //group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"
    private const val HEADER_GROUP = "(^#{1,6} .+?$)"
    private const val QUOTE_GROUP = "(^> .+?$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    private const val BOLD_GROUP =
        "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!\\*)\\~{2}[^~].*?[^~]?\\~{2}(?!\\*))"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?]\\(.+?\\)|^\\[*?]\\(.*?\\))"
    private const val ORDERED_LIST_ITEM_GROUP = "(^\\d+\\. .+\$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"
    private const val BLOCK_GROUP = "((?<!`)`{3}[^` \\\\s][\\w|\\W]*?[^`\\\\s]?`{3}(?!`))"
    private const val IMAGE_GROUP = "(\\!\\[[^\\[\\]]*?]\\(.+?\\))"


    //result regex
    private const val MARKDOWN_GROUPS = "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP" +
            "|$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP|$INLINE_GROUP|$LINK_GROUP|$ORDERED_LIST_ITEM_GROUP|$BLOCK_GROUP|$IMAGE_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    /**
     * clear markdown text to string without markdown characters
     */
    fun clear(string: String?): String? {
        //TODO implement me
        return null
    }

    /**
     * find markdown elements in markdown text
     */
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0
        var orderIndex = 0;

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            //if something is found then everything before - TEXT
            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            //found text
            var text: CharSequence

            //group range for iterate by groups
            val groups = 1..12
            var group = -1
            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }

            when (group) {
                //NOT FOUND -> BREAK
                -1 -> break@loop

                //UNORDERED LIST
                1 -> {
                    //text without "*. "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    //find inner elements
                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)

                    //next find start from position "endIndex" (last regex character)
                    lastStartIndex = endIndex
                }

                //HEADER
                2 -> {
                    val reg = "^#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length

                    //text without "{#} "
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)

                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //QUOTE
                3 -> {
                    //text without "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    val subelements = findElements(text)
                    val element = Element.Quote(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //ITALIC
                4 -> {
                    //text without "*{}*"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val subelements = findElements(text)
                    val element = Element.Italic(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //BOLD
                5 -> {
                    //text without "**()**"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))
                    val subelements = findElements(text)
                    val element = Element.Bold(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //STRIKE
                6 -> {
                    //text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))
                    val subelements = findElements(text)
                    val element = Element.Strike(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //RULE
                7 -> {
                    //text without "***" insert empty character
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //INLINE CODE
                8 -> {
                    //text without "`{}`"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val element = Element.InlineCode(text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //LINK
                9 -> {
                    //full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    val (title: String, link: String) = "\\[(.*)]\\((.*)\\)".toRegex()
                        .find(text)!!.destructured
                    val element = Element.Link(link, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //ORDERED LIST
                10 -> {
                    //text without "\\d. "
                    orderIndex += 1
                    text = string.subSequence(
                        startIndex.plus(2 + orderIndex.toString().countDigits()),
                        endIndex
                    )
                    val subs = findElements(text)

                    val element =
                        Element.OrderedListItem((orderIndex).toString() + ".", text, subs)
                    parents.add(element)

                    lastStartIndex = endIndex
                }

                //BLOCK CODE
                11 -> {
                    //text without "```{}```"
                    text =
                        string.subSequence(startIndex.inc().inc().inc(), endIndex.dec().dec().dec())
                    val element = Element.BlockCode(Element.BlockCode.Type.MIDDLE, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //IMAGE
                12 -> {
                    //full text for regex
                    text = string.subSequence(startIndex, endIndex)

                    var (alt: String?, url: String, title: String?) = "!\\[(.*)\\]\\((.*?[^ \\\"\\)]+)( \\\".*?\\\")*".toRegex()
                        .find(text)!!.destructured

                    if (alt.length == 0) alt = null.toString()

                    val element = Element.Image(title, url, alt, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
            }

        }

        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }

        return parents
    }
}

data class MarkdownText(val elements: List<Element>)

sealed class Element() {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule(
        override val text: CharSequence = "***", //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence, //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence, //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        val type: Type = Type.MIDDLE,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element() {
        enum class Type { START, END, MIDDLE, SINGLE }
    }

    data class Image(
        val title: String,
        val url: String,
        val alt: String,
        override val text: CharSequence,
        override val elements: ArrayList<Element> = ArrayList()
    ) : Element()
}