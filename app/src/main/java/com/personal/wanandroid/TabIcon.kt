package com.personal.wanandroid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Decorative icons: NavigationBarItem text provides accessible labels. */
@Composable
internal fun TabIcon(index: Int) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val unit = size.width / 24
        val stroke = Stroke(1.8f * unit)
        when (index) {
            0 -> {
                val roof = Path().apply {
                    moveTo(3 * unit, 11 * unit)
                    lineTo(12 * unit, 3 * unit)
                    lineTo(21 * unit, 11 * unit)
                }
                drawPath(roof, color, style = stroke)
                drawRect(
                    color,
                    Offset(6 * unit, 11 * unit),
                    Size(12 * unit, 10 * unit),
                    style = stroke
                )
            }

            1 -> {
                for (x in listOf(3, 14)) {
                    for (y in listOf(3, 14)) {
                        drawRect(
                            color,
                            Offset(x * unit, y * unit),
                            Size(7 * unit, 7 * unit),
                            style = stroke
                        )
                    }
                }
            }

            else -> {
                drawCircle(color, 4 * unit, Offset(12 * unit, 7 * unit), style = stroke)
                drawArc(
                    color,
                    180f,
                    180f,
                    false,
                    Offset(4 * unit, 14 * unit),
                    Size(
                        16 * unit,
                        12 * unit
                    ),
                    style = stroke
                )
            }
        }
    }
}
