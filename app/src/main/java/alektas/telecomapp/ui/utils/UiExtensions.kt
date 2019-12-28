package alektas.telecomapp.ui.utils

import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import java.text.NumberFormat

fun GraphView.setupLabels(
    xIntMin: Int = 1,
    xIntMax: Int,
    xFracMin: Int = 0,
    xFracMax: Int = 0,
    yIntMin: Int = 1,
    yIntMax: Int,
    yFracMin: Int = 0,
    yFracMax: Int = 0
) {
    val xFormat = NumberFormat.getInstance().apply {
        minimumFractionDigits = xFracMin
        maximumFractionDigits = xFracMax
        minimumIntegerDigits = xIntMin
        maximumIntegerDigits = xIntMax
    }
    val yFormat = NumberFormat.getInstance().apply {
        minimumFractionDigits = yFracMin
        maximumFractionDigits = yFracMax
        minimumIntegerDigits = yIntMin
        maximumIntegerDigits = yIntMax
    }
    gridLabelRenderer.labelFormatter = DefaultLabelFormatter(xFormat, yFormat)
}