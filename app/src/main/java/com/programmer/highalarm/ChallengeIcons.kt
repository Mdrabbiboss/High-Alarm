package com.programmer.highalarm

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/*
 * Custom pictograms for challenges that Material has no clear icon for.
 * Drawn on a 24x24 grid in a single colour so they tint (and take the
 * 3D gradient in the challenge grid) exactly like Material icons.
 */

private fun ImageVector.Builder.stroke(width: Float, block: PathBuilder.() -> Unit) =
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block
    )

private fun ImageVector.Builder.dot(cx: Float, cy: Float, r: Float) =
    path(fill = SolidColor(Color.Black)) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
        arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
        close()
    }

/** Side view of a person in the push-up (plank) position. */
val PushUpIcon: ImageVector by lazy {
    ImageVector.Builder("PushUp", 24.dp, 24.dp, 24f, 24f).apply {
        stroke(1.3f) { moveTo(1.8f, 19.6f); lineTo(22.2f, 19.6f) }               // floor
        stroke(2.6f) { moveTo(16.2f, 11.2f); lineTo(9.5f, 14.3f); lineTo(3.4f, 17.4f) } // straight body
        stroke(2.0f) { moveTo(16.2f, 11.2f); lineTo(16.4f, 15.2f); lineTo(17.4f, 18.4f) } // supporting arm
        dot(19.2f, 9.4f, 2.2f)                                                   // head
    }.build()
}

/** Person at the bottom of a squat, arms forward. */
val SquatIcon: ImageVector by lazy {
    ImageVector.Builder("Squat", 24.dp, 24.dp, 24f, 24f).apply {
        stroke(1.3f) { moveTo(2f, 21.2f); lineTo(22f, 21.2f) }                   // floor
        stroke(2.6f) { moveTo(10.6f, 7.6f); lineTo(8.6f, 13.4f) }                // torso
        stroke(2.4f) { moveTo(8.6f, 13.4f); lineTo(14.6f, 13.9f); lineTo(13.6f, 19.6f) } // thigh + shin
        stroke(2.0f) { moveTo(10.4f, 8.8f); lineTo(17.8f, 9.4f) }                // arms forward
        dot(11.6f, 4.6f, 2.2f)                                                   // head
    }.build()
}
