package com.logicminigames.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight custom Material-style vector icons to keep the APK well below 10 MB
 * without bundling material-icons-extended.
 */
object AppIcons {

    val Play: ImageVector by lazy {
        icon("Play") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 5f)
                verticalLineTo(19f)
                lineTo(19f, 12f)
                close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 5f)
                horizontalLineTo(10f)
                verticalLineTo(19f)
                horizontalLineTo(6f)
                close()
                moveTo(14f, 5f)
                horizontalLineTo(18f)
                verticalLineTo(19f)
                horizontalLineTo(14f)
                close()
            }
        }
    }

    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19f, 12f)
                horizontalLineTo(5f)
                moveTo(12f, 19f)
                lineTo(5f, 12f)
                lineTo(12f, 5f)
            }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    val Undo: ImageVector by lazy {
        icon("Undo") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 14f)
                lineTo(4f, 9f)
                lineTo(9f, 4f)
                moveTo(4f, 9f)
                horizontalLineTo(15f)
                curveTo(18.3f, 9f, 20f, 11.2f, 20f, 14f)
                curveTo(20f, 16.8f, 18.3f, 19f, 15f, 19f)
                horizontalLineTo(11f)
            }
        }
    }

    val Redo: ImageVector by lazy {
        icon("Redo") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 14f)
                lineTo(20f, 9f)
                lineTo(15f, 4f)
                moveTo(20f, 9f)
                horizontalLineTo(9f)
                curveTo(5.7f, 9f, 4f, 11.2f, 4f, 14f)
                curveTo(4f, 16.8f, 5.7f, 19f, 9f, 19f)
                horizontalLineTo(13f)
            }
        }
    }

    val Backspace: ImageVector by lazy {
        icon("Backspace") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 5f)
                horizontalLineTo(20f)
                curveTo(21.1f, 5f, 22f, 5.9f, 22f, 7f)
                verticalLineTo(17f)
                curveTo(22f, 18.1f, 21.1f, 19f, 20f, 19f)
                horizontalLineTo(9f)
                lineTo(3f, 12f)
                close()
                moveTo(12f, 9f)
                lineTo(18f, 15f)
                moveTo(18f, 9f)
                lineTo(12f, 15f)
            }
        }
    }

    val Edit: ImageVector by lazy {
        icon("Edit") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 20f)
                horizontalLineTo(8f)
                lineTo(19f, 9f)
                lineTo(15f, 5f)
                lineTo(4f, 16f)
                close()
                moveTo(13.5f, 6.5f)
                lineTo(17.5f, 10.5f)
            }
        }
    }

    val Lightbulb: ImageVector by lazy {
        icon("Lightbulb") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 18f)
                horizontalLineTo(15f)
                moveTo(10f, 21f)
                horizontalLineTo(14f)
                moveTo(15f, 15.5f)
                curveTo(16.8f, 14.3f, 18f, 12.3f, 18f, 10f)
                curveTo(18f, 6.7f, 15.3f, 4f, 12f, 4f)
                curveTo(8.7f, 4f, 6f, 6.7f, 6f, 10f)
                curveTo(6f, 12.3f, 7.2f, 14.3f, 9f, 15.5f)
                verticalLineTo(18f)
            }
        }
    }

    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 21f)
                curveTo(17f, 21f, 21f, 17f, 21f, 12f)
                curveTo(21f, 7f, 17f, 3f, 12f, 3f)
                curveTo(7f, 3f, 3f, 7f, 3f, 12f)
                curveTo(3f, 17f, 7f, 21f, 12f, 21f)
                close()
                moveTo(8.5f, 12.5f)
                lineTo(11f, 15f)
                lineTo(16f, 9.5f)
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 12.5f)
                lineTo(10f, 17.5f)
                lineTo(19f, 7f)
            }
        }
    }

    val Trophy: ImageVector by lazy {
        icon("Trophy") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 4f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                curveTo(17f, 12.8f, 14.8f, 15f, 12f, 15f)
                curveTo(9.2f, 15f, 7f, 12.8f, 7f, 10f)
                close()
                moveTo(7f, 6f)
                horizontalLineTo(4f)
                verticalLineTo(9f)
                curveTo(4f, 10.7f, 5.3f, 12f, 7f, 12f)
                moveTo(17f, 6f)
                horizontalLineTo(20f)
                verticalLineTo(9f)
                curveTo(20f, 10.7f, 18.7f, 12f, 17f, 12f)
                moveTo(12f, 15f)
                verticalLineTo(20f)
                moveTo(8f, 20f)
                horizontalLineTo(16f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 10f)
                horizontalLineTo(20f)
                moveTo(8f, 3f)
                verticalLineTo(6f)
                moveTo(16f, 3f)
                verticalLineTo(6f)
            }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
                curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                close()
                moveTo(12f, 3f)
                verticalLineTo(5f)
                moveTo(12f, 19f)
                verticalLineTo(21f)
                moveTo(3f, 12f)
                horizontalLineTo(5f)
                moveTo(19f, 12f)
                horizontalLineTo(21f)
                moveTo(5.6f, 5.6f)
                lineTo(7f, 7f)
                moveTo(17f, 17f)
                lineTo(18.4f, 18.4f)
                moveTo(18.4f, 5.6f)
                lineTo(17f, 7f)
                moveTo(7f, 17f)
                lineTo(5.6f, 18.4f)
            }
        }
    }

    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20f, 12f)
                curveTo(20f, 16.4f, 16.4f, 20f, 12f, 20f)
                curveTo(7.6f, 20f, 4f, 16.4f, 4f, 12f)
                curveTo(4f, 7.6f, 7.6f, 4f, 12f, 4f)
                curveTo(15f, 4f, 17.6f, 5.6f, 19f, 8f)
                moveTo(20f, 4f)
                verticalLineTo(8.5f)
                horizontalLineTo(15.5f)
            }
        }
    }

    val VisibilityOff: ImageVector by lazy {
        icon("VisibilityOff") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
                moveTo(10.6f, 10.6f)
                curveTo(10.2f, 11f, 10f, 11.5f, 10f, 12f)
                curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                curveTo(12.5f, 14f, 13f, 13.8f, 13.4f, 13.4f)
                moveTo(6.5f, 6.5f)
                curveTo(4.6f, 7.8f, 3.2f, 9.7f, 2.5f, 12f)
                curveTo(4f, 16.5f, 7.7f, 19f, 12f, 19f)
                curveTo(13.9f, 19f, 15.7f, 18.4f, 17.2f, 17.3f)
                moveTo(9.9f, 5.3f)
                curveTo(10.6f, 5.1f, 11.3f, 5f, 12f, 5f)
                curveTo(16.3f, 5f, 20f, 7.5f, 21.5f, 12f)
                curveTo(21f, 13.4f, 20.3f, 14.6f, 19.3f, 15.6f)
            }
        }
    }

    val Grid: ImageVector by lazy {
        icon("Grid") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(4f, 4f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
                moveTo(9.33f, 4f)
                verticalLineTo(20f)
                moveTo(14.67f, 4f)
                verticalLineTo(20f)
                moveTo(4f, 9.33f)
                horizontalLineTo(20f)
                moveTo(4f, 14.67f)
                horizontalLineTo(20f)
            }
        }
    }

    val Flame: ImageVector by lazy {
        icon("Flame") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                curveTo(15f, 6.5f, 18f, 9.5f, 18f, 13.5f)
                curveTo(18f, 17f, 15.3f, 20f, 12f, 20f)
                curveTo(8.7f, 20f, 6f, 17f, 6f, 13.5f)
                curveTo(6f, 11f, 7.2f, 9f, 8.5f, 7.5f)
                curveTo(8.8f, 9.5f, 10f, 10.5f, 11f, 10.5f)
                curveTo(10.5f, 8f, 11f, 5.5f, 12f, 3f)
                close()
            }
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 11f)
                horizontalLineTo(18f)
                verticalLineTo(20f)
                horizontalLineTo(6f)
                close()
                moveTo(8.5f, 11f)
                verticalLineTo(8f)
                curveTo(8.5f, 6.1f, 10.1f, 4.5f, 12f, 4.5f)
                curveTo(13.9f, 4.5f, 15.5f, 6.1f, 15.5f, 8f)
                verticalLineTo(11f)
            }
        }
    }

    private inline fun icon(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()
}
