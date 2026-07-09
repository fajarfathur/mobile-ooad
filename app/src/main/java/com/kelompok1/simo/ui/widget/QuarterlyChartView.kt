package com.kelompok1.simo.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.kelompok1.simo.R

/**
 * QuarterlyChartView — Custom Canvas bar chart interaktif
 * - Animasi masuk smooth
 * - Tap bar → highlight + callback
 * - Grid lines + value labels
 * - Realtime update via setData()
 */
class QuarterlyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class BarEntry(
        val label: String,
        val value: Float,
        val valueFmt: String,
        val colorRes: Int
    )

    var onBarTapped: ((BarEntry, Int) -> Unit)? = null

    private val entries = mutableListOf<BarEntry>()
    private var animProgress = 0f
    private var selectedIdx = -1

    // Paint objects
    private val barPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5E7EB")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val labelPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val valuePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111827")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val selectedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // Layout
    private val paddingH = 48f
    private val paddingTop = 24f
    private val labelH = 52f
    private val valueH = 44f
    private val barRadius = 18f

    private val barRects = mutableListOf<RectF>()

    fun setData(newEntries: List<BarEntry>, animate: Boolean = true) {
        entries.clear()
        entries.addAll(newEntries)
        barRects.clear()
        if (animate) startAnimation() else { animProgress = 1f; invalidate() }
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
        }.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val chartBottom = h - labelH - 4f
        val chartTop    = paddingTop + valueH
        val chartH      = chartBottom - chartTop
        val maxVal      = entries.maxOf { it.value }.coerceAtLeast(1f)

        val barW  = (w - paddingH * 2) / entries.size
        val barPad = barW * 0.22f
        barRects.clear()

        // Grid lines
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = chartTop + chartH / gridCount * i
            canvas.drawLine(paddingH, y, w - paddingH, y, gridPaint)
        }

        // Bars
        entries.forEachIndexed { idx, entry ->
            val barColor = ContextCompat.getColor(context, entry.colorRes)
            val left  = paddingH + barW * idx + barPad
            val right = paddingH + barW * (idx + 1) - barPad
            val barH  = chartH * (entry.value / maxVal) * animProgress
            val top   = chartBottom - barH
            val rect  = RectF(left, top, right, chartBottom)
            barRects.add(rect)

            // Selected: lighter + ring
            val alpha = if (selectedIdx == idx) 255 else if (selectedIdx == -1) 255 else 160
            barPaint.color = barColor
            barPaint.alpha = alpha

            // Gradient fill
            barPaint.shader = LinearGradient(
                left, top, right, chartBottom,
                intArrayOf(lighten(barColor, 0.3f), barColor),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, barRadius, barRadius, barPaint)
            barPaint.shader = null

            // Selected ring
            if (selectedIdx == idx) {
                selectedRingPaint.color = barColor
                selectedRingPaint.alpha = 180
                canvas.drawRoundRect(
                    RectF(rect.left - 3, rect.top - 3, rect.right + 3, rect.bottom + 3),
                    barRadius + 3, barRadius + 3, selectedRingPaint
                )
            }

            // Value label di atas bar
            if (animProgress > 0.6f) {
                valuePaint.color = barColor
                valuePaint.alpha = ((animProgress - 0.6f) / 0.4f * 255).toInt()
                canvas.drawText(
                    entry.valueFmt,
                    (left + right) / 2,
                    top - 8f,
                    valuePaint
                )
            }

            // Label di bawah
            labelPaint.color = if (selectedIdx == idx)
                ContextCompat.getColor(context, R.color.primary)
            else Color.parseColor("#6B7280")
            canvas.drawText(
                entry.label,
                (left + right) / 2,
                chartBottom + labelH - 8f,
                labelPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            barRects.forEachIndexed { idx, rect ->
                if (event.x in rect.left..rect.right &&
                    event.y >= rect.top - 40f && event.y <= rect.bottom + 20f) {
                    selectedIdx = if (selectedIdx == idx) -1 else idx
                    invalidate()
                    if (selectedIdx == idx) {
                        onBarTapped?.invoke(entries[idx], idx)
                    }
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
