package com.kelompok1.simo.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import android.animation.AnimatorSet

/**
 * ShimmerView — animasi shimmer loading placeholder
 * Pakai sebelum data real-time dari Supabase tiba
 */
class ShimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shimmerX = -1f
    private val cornerRadius = 16f

    init {
        startShimmer()
    }

    private fun startShimmer() {
        val anim = ValueAnimator.ofFloat(-1f, 2f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                shimmerX = it.animatedValue as Float
                invalidate()
            }
        }
        anim.start()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val gradient = LinearGradient(
            shimmerX * w, 0f, (shimmerX + 0.5f) * w, h,
            intArrayOf(
                Color.parseColor("#F3F4F6"),
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#F3F4F6")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        shimmerPaint.shader = gradient
        canvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, shimmerPaint)
    }
}
