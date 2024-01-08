package ru.skillbranch.skillarticles.markdown

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.attrValue
import ru.skillbranch.skillarticles.extensions.dpToPx
import ru.skillbranch.skillarticles.markdown.spans.*
import ru.skillbranch.skillarticles.ui.delegates.AttrValue

class MarkdownBuilder(context: Context) {
    private val colorSecondary = AttrValue(R.attr.colorSecondary)
    private val colorPrimary = AttrValue(R.attr.colorPrimary)
    private val colorOnSurface = AttrValue(R.attr.colorOnSurface)
    private val opacityColorSurface = context.getColor(R.color.opacity_color_surface)
    private val colorDivider = context.getColor(R.color.color_divider)
    private val gap: Float = context.dpToPx(8)
    private val bulletRadius = context.dpToPx(4)
    private val strikeWidth = context.dpToPx(4)
    private val headerMarginTop = context.dpToPx(12)
    private val headerMarginBottom = context.dpToPx(8)
    private val ruleWidth = context.dpToPx(2)
    private val cornerRadius = context.dpToPx(8)
    private val linkIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_link_24)!!

    fun markdownToSpan(string: String): SpannedString {
        //TODO implement me
    }

    private fun buildElement(element: Element, builder: SpannableStringBuilder): CharSequence {
        //TODO implement me
    }
}