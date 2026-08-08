package jp.co.soracom.qlm29hrtk.ui.map

import org.maplibre.android.style.expressions.Expression

internal const val SOURCE_POINTS = "track-points"
internal const val SOURCE_LINE = "track-line"
internal const val LAYER_LINE = "track-line-layer"
internal const val LAYER_LATEST = "track-latest-layer"
internal const val SOURCE_SP_POINTS = "smartphone-points"
internal const val SOURCE_SP_LINES = "smartphone-lines"
internal const val LAYER_SP_POINTS = "smartphone-points-layer"
internal const val LAYER_SP_LINES = "smartphone-lines-layer"
internal const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
internal const val CACHE_LIMIT_BYTES = 200L * 1024 * 1024

/** MAP-04: quality colors are product semantics, not theme colors. */
internal val QUALITY_COLORS = mapOf(
    0 to "#808080", // No Fix
    1 to "#F44336", // SPS
    2 to "#2196F3", // DGPS
    4 to "#4CAF50", // Fixed
    5 to "#FFEB3B", // Float
    6 to "#FF9800", // Dead reckoning
)
internal const val SP_POINT_COLOR = "#FF8A80"
internal const val SP_LINE_COLOR = "#808080"
internal val QUALITY_LABELS = listOf(0 to "No Fix", 1 to "SPS", 2 to "DGPS", 5 to "Float", 4 to "Fixed", 6 to "DR")

/** MAP-05: low zoom removes overlapping white borders so quality remains legible. */
internal fun zoomedRadius(compact: Double, detailed: Double): Expression =
    Expression.interpolate(
        Expression.linear(),
        Expression.zoom(),
        Expression.stop(12, compact),
        Expression.stop(14, compact),
        Expression.stop(16, detailed * 0.75),
        Expression.stop(17.5, detailed),
    )

internal fun zoomedStroke(detailed: Double): Expression =
    Expression.interpolate(
        Expression.linear(),
        Expression.zoom(),
        Expression.stop(12, 0.0),
        Expression.stop(14, 0.0),
        Expression.stop(15.5, detailed * 0.25),
        Expression.stop(17.5, detailed),
    )
