package alektas.telecomapp.ui.chart.polar

import alektas.telecomapp.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import kotlin.math.abs
import kotlin.math.min

class PolarGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var data: List<Pair<Float, Float>>? = null
    private var pointRadius: Float = 4f
    private var graphSize: Float = 0f
    private var centerY: Float = 0f
    private var centerX: Float = 0f
    private var left: Float = 0f
    private var right: Float = 0f
    private var top: Float = 0f
    private var bottom: Float = 0f
    private var axisLabelX: String = "x"
    private var axisLabelY: String = "y"
    private val axisPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
    }
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = LABELS_SIZE_DEFAULT.toFloat()
    }
    private val pointPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PolarGraphView,
            0, 0
        ).apply {
            try {
                axisLabelX = getString(R.styleable.PolarGraphView_label_x) ?: "x"
                axisLabelY = getString(R.styleable.PolarGraphView_label_y) ?: "y"
                textPaint.textSize =
                    getDimensionPixelSize(
                        R.styleable.PolarGraphView_labels_text_size,
                        LABELS_SIZE_DEFAULT
                    ).toFloat()
                pointPaint.color = getInt(R.styleable.PolarGraphView_points_color, Color.BLUE)
            } finally {
                recycle()
            }
        }
    }

    companion object {
        @FloatRange(from = 0.0, to = 1.0)
        private const val GRAPH_DATA_SCALE = 0.8f
        private const val LABELS_SIZE_DEFAULT: Int = 48
        private const val OFFSET: Int = 20
    }

    fun setData(points: List<Pair<Float, Float>>) {
        data = points

        postInvalidate()
    }

    private fun normalize(
        points: List<Pair<Float, Float>>
    ): List<Pair<Float, Float>> {
        if (graphSize == 0f || points.isEmpty()) return emptyList()

        val max = points.flatMap { listOf(it.first, it.second) }
            .maxBy { abs(it) } ?: 0f
        val k = GRAPH_DATA_SCALE * (graphSize / 2) / max

        return points.map {
            Pair(it.first * k, it.second * k)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        graphSize = min(
            height - paddingTop - paddingBottom,
            width - paddingLeft - paddingRight
        ).toFloat() - 2 * OFFSET - textPaint.fontSpacing
        centerY = (height + paddingTop - paddingBottom + textPaint.fontSpacing) / 2f
        centerX = (width + paddingLeft - paddingRight) / 2f
        left = centerX - graphSize / 2
        right = centerX + graphSize / 2
        top = centerY - graphSize / 2
        bottom = centerY + graphSize / 2

        data?.let { data = normalize(it) }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            // draw grid
            drawLines(
                floatArrayOf(
                    left, top, right, top,
                    right, top, right, bottom,
                    right, bottom, left, bottom,
                    left, bottom, left, top,
                    left, centerY, right, centerY,
                    centerX, top, centerX, bottom
                ), axisPaint
            )

            // draw labels
            drawText(axisLabelX, right + OFFSET, centerY + textPaint.textSize / 2, textPaint)
            drawText(axisLabelY, centerX - OFFSET, top - OFFSET, textPaint)

            // draw points
            data?.forEach {
                drawCircle(centerX + it.first, centerY + it.second, pointRadius, pointPaint)
            }
        }
    }
}