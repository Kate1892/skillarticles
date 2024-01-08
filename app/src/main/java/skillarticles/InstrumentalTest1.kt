package ru.skillbranch.skillarticles

import android.graphics.*
import android.graphics.drawable.VectorDrawable
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.view.View.TEXT_DIRECTION_FIRST_STRONG
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.skillbranch.skillarticles.markdown.spans.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

@RunWith(AndroidJUnit4::class)
class InstrumentalTest1 {
    companion object {
        var scaledDensity: Float = 0f

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            scaledDensity =
                ApplicationProvider.getApplicationContext<App>().resources.displayMetrics.scaledDensity
        }
    }

    //defaults
    private val defaultColor = Color.GRAY
    private val currentMargin = 0
    private val lineTop = 0 //lineTop for line under test
    private val lineBase = 14.dp() //lineBase for line under test
    private val lineBottom = 20.dp() //lineBottom for line under test
    private val defaultFontAscent = (-10).dp() //16sp fontsize
    private val defaultFontDescent = (4).dp() //16sp fontsize
    private val canvasWidth = 360.dp()

    //mocks
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private lateinit var layout: Layout

    //text under test
    private lateinit var text: SpannableString

    //overwrite mocks before each test
    @Before
    fun setup() {
        canvas = mockk(relaxed = true) { //overwrite only need functions other functions relaxed
            every { getWidth() } returns canvasWidth
        }
        paint = mockk(relaxed = true) {
            every { getColor() } returns defaultColor
        }
        layout = mockk(relaxed = true)

        text = SpannableString("text")
    }

    @Test
    fun unordered_list() {
        //settings
        val color = Color.RED
        val gap = 8.dpf()
        val radius = 4.dpf()

        //set span on text
        val span = UnorderedListSpan(gap, radius, color)
        text.setSpan(span, 0, 0, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        //check leading margin
        assertEquals((4 * radius + gap).toInt(), span.getLeadingMargin(true))


        //bullet draw
        span.drawLeadingMargin(
            canvas, paint, currentMargin, TEXT_DIRECTION_FIRST_STRONG,
            lineTop, lineBase, lineBottom, text, 0, text.length,
            true, layout
        )

        //check order call
        verifyOrder {
            //check first set color to paint
            paint.color = color
            //check draw circle bullet
            canvas.drawCircle(
                gap + currentMargin + radius,
                (lineTop + lineBottom) / 2f,
                radius, paint
            )
            //check paint color restore
            paint.color = defaultColor
        }
    }

    @Test
    fun ordered_list() {
        //settings
        val color = Color.RED
        val gap = 12.dpf()
        val order = "1"

        //set span on text
        val span = OrderedListSpan(gap, order, color)
        text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        //check leading margin
        Assert.assertEquals((order.length.inc() * gap).toInt(), span.getLeadingMargin(true))

        //check bullet draw
        span.drawLeadingMargin(
            canvas, paint, currentMargin, TEXT_DIRECTION_FIRST_STRONG,
            lineTop, lineBase, lineBottom, text, 0, text.length,
            true, layout
        )

        //check order call
        verifyOrder {
            //check first set color to paint
            paint.color = color

            //check draw order bullet
            canvas.drawText(
                order,
                currentMargin + gap,
                lineBase.toFloat(),
                paint
            )

            //check paint color restore
            paint.color = defaultColor
        }
    }

    @Test
    fun draw_quote() {
        //settings
        val color = Color.RED
        val gap = 8.dpf()
        val lineWidth = 4.dpf()

        val span = BlockquotesSpan(gap, lineWidth, color)
        text.setSpan(span, 0, 0, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        //check leading margin
        assertEquals((lineWidth + gap).toInt(), span.getLeadingMargin(true))

        //check line draw
        span.drawLeadingMargin(
            canvas, paint, currentMargin, TEXT_DIRECTION_FIRST_STRONG,
            lineTop, lineBase, lineBottom, text, 0, text.length,
            true, layout
        )

        //check order call
        verifyOrder {
            //check first set color to painyt
            paint.color = color
            paint.strokeWidth = lineWidth
            //check draw line
            canvas.drawLine(
                lineWidth / 2f,
                lineTop.toFloat(),
                lineWidth / 2,
                lineBottom.toFloat(),
                paint
            )

            //check paint color restore
            paint.color = defaultColor
        }

    }

    @Test
    fun draw_header() {
        //settings
        val levels = 1..6
        val textColor = Color.RED
        val lineColor = Color.GREEN
        val marginTop = 12.dpf()
        val marginBottom = 8.dpf()

        //mocks
        val measurePaint = mockk<TextPaint>(relaxed = true)
        val drawPaint = mockk<TextPaint>(relaxed = true)
        val fm = Paint.FontMetricsInt()

        for (level in levels) {

            val span = HeaderSpan(level, textColor, lineColor, marginTop, marginBottom)
            text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            //check leading margin
            assertEquals(0, span.getLeadingMargin(true))

            //check measure state
            span.updateMeasureState(measurePaint)
            verifyAll {
                measurePaint.textSize *= span.sizes[level]!!
                measurePaint.isFakeBoldText = true
            }

            //check draw state
            span.updateDrawState(drawPaint)
            verifyAll {
                drawPaint.textSize *= span.sizes[level]!!
                drawPaint.isFakeBoldText = true
                drawPaint.color = textColor
            }

            //check change line height
            fm.ascent = defaultFontAscent
            fm.descent = defaultFontDescent

            span.chooseHeight(text, 0, text.length.inc(), 0, 0, fm)
            //check top
            assertEquals((defaultFontAscent - marginTop).toInt(), fm.ascent)
            //check bottom
            assertEquals(
                ((defaultFontDescent - defaultFontAscent) * span.linePadding + marginBottom).toInt(),
                fm.descent
            )

            assertEquals(fm.top, fm.ascent)
            assertEquals(fm.bottom, fm.descent)

            //check line draw
            span.drawLeadingMargin(
                canvas, paint, currentMargin, TEXT_DIRECTION_FIRST_STRONG,
                lineTop, lineBase, lineBottom, text, 0, text.length,
                true, layout
            )

            //check draw line for first, second level header
            if (level == 1 || level == 2) {
                val lh = (paint.descent() - paint.ascent()) * span.sizes[level]!!
                val lineOffset = lineBase + lh * span.linePadding

                verifyOrder {
                    //check set line color
                    paint.color = lineColor
                    //check draw line under header
                    canvas.drawLine(0f, lineOffset, canvasWidth.toFloat(), lineOffset, paint)
                    //check restore paint color
                    paint.color = defaultColor
                }

            }

        }

    }

    @Test
    fun draw_rule() {
        //settings
        val color = Color.RED
        val width = 2.dpf()

        val span = HorizontalRuleSpan(width, color)
        text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        //check draw rule line
        span.draw(
            canvas,
            text,
            0,
            text.length,
            currentMargin.toFloat(),
            lineTop,
            lineBase,
            lineBottom,
            paint
        )

        //verify order call
        verifyOrder {
            //check set rule color
            paint.color = color
            //check draw line
            canvas.drawLine(
                0f,
                (lineTop + lineBottom) / 2f,
                canvasWidth.toFloat(),
                (lineTop + lineBottom) / 2f,
                paint
            )
            //check paint color restore
            paint.color = defaultColor

        }

    }

    @Test
    fun draw_inline_code() {
        //settings
        val textColor = Color.RED
        val bgColor = Color.GREEN
        val cornerRadius = 8.dpf()
        val padding = 8.dpf()
        val measureText = 100f

        //mocks
        every { paint.measureText(any<String>(), any(), any()) } returns measureText

        val fm = Paint.FontMetricsInt()

        val span = InlineCodeSpan(textColor, bgColor, cornerRadius, padding)
        text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        //check span size
        val size = span.getSize(paint, text, 0, text.length, fm)
        assertEquals((2 * padding + measureText).toInt(), size)

        //check draw inline code
        span.draw(
            canvas,
            text,
            0,
            text.length,
            currentMargin.toFloat(),
            lineTop,
            lineBase,
            lineBottom,
            paint
        )

        //check call order
        verifyOrder {
            //check first set color to paint for background
            paint.color = bgColor
            //check draw background
            canvas.drawRoundRect(
                RectF(
                    0f,
                    lineTop.toFloat(),
                    measureText + 2 * padding,
                    lineBottom.toFloat()
                ),
                cornerRadius, cornerRadius,
                paint
            )

            //check set text color
            paint.color = textColor
            //check draw text
            canvas.drawText(
                text, 0, text.length, currentMargin + padding,
                lineBase.toFloat(),
                paint
            )

            //check paint color restore
            paint.color = defaultColor
        }

    }

    @Test
    fun draw_link() {
        //settings
        val gap = 8.dpf()
        val textColor = Color.BLUE
        val dotWidth = 4.dpf()

        //defaults
        val fm = Paint.FontMetricsInt()
        fm.ascent = defaultFontAscent
        fm.descent = defaultFontDescent
        val measureText = 100f

        //mocks
        every { paint.measureText(any<String>(), any(), any()) } returns measureText

        //spy
        val linkDrawable = spyk(VectorDrawable()) {
            every { draw(canvas) } answers {} //Unit do nothing if draw on canvas in spy object
        }
        val path = spyk(Path())

        val span = IconLinkSpan(linkDrawable, gap, textColor, dotWidth)
        text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        //setter for mock path
        span.path = path

        //check measure size
        val size = span.getSize(paint, text, 0, text.length, fm)
        val iconSize = defaultFontDescent - defaultFontAscent
        assertEquals((iconSize + gap + measureText).toInt(), size)

        //check drawable set bounds
        verify {
            linkDrawable.setBounds(
                0,
                0,
                iconSize,
                iconSize
            )
        }

        //check draw icon
        span.draw(
            canvas, text, 0, text.length, currentMargin.toFloat(),
            lineTop, lineBase, lineBottom,
            paint
        )

        //check path effect
        verifyOrder {
            paint.pathEffect = any()
            paint.strokeWidth = 0f
            paint.color = textColor
        }

        //check reset path and draw line under text
        verifyOrder {
            path.reset()
            path.moveTo(currentMargin + iconSize + gap, lineBottom.toFloat())
            path.lineTo(currentMargin + iconSize + gap + measureText, lineBottom.toFloat())
            canvas.drawPath(path, paint)
        }

        //check draw icon
        verifyOrder {
            canvas.save()
            canvas.translate(
                currentMargin + gap / 2f,
                lineBottom - linkDrawable.bounds.bottom.toFloat()
            )
            linkDrawable.draw(canvas)
            canvas.restore()
        }

        //check draw text
        verifyOrder {
            //set text color
            paint.color = textColor
            //check draw text
            canvas.drawText(
                text, 0, text.length,
                currentMargin + iconSize + gap,
                lineBase.toFloat(),
                paint
            )
            //check restore color
            paint.color = defaultColor
        }


    }

    fun under_line() {
        //settings
        val textColor = Color.BLUE
        val dotWidth = 4.dpf()

        //defaults
        val fm = Paint.FontMetricsInt()
        fm.ascent = defaultFontAscent
        fm.descent = defaultFontDescent
        val measureText = 100f

        //mocks
        every { paint.measureText(any<String>(), any(), any()) } returns measureText

        val path = spyk(Path())

        val span = UnderlineSpan(textColor, dotWidth)
        span.path = path
        text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)


        //check measure size
        val size = span.getSize(paint, text, 0, text.length, fm)
        assertEquals((measureText).toInt(), size)

        //check draw icon
        span.draw(
            canvas, text, 0, text.length, currentMargin.toFloat(),
            lineTop, lineBase, lineBottom,
            paint
        )
        //check reset path and draw line under text
        verifyOrder {
            paint.pathEffect = any()
            paint.color = textColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0f

            path.reset()
            path.moveTo(currentMargin.toFloat(), lineBottom.toFloat())
            path.lineTo(currentMargin + measureText, lineBottom.toFloat())
            canvas.drawPath(path, paint)

            //check restore color
            paint.color = defaultColor
            //check draw text
            canvas.drawText(
                text, 0, text.length,
                currentMargin.toFloat(),
                lineBase.toFloat(),
                paint
            )

        }
    }


    private fun Int.dp() = (this * scaledDensity).toInt()
    private fun Int.dpf() = this * scaledDensity
}





