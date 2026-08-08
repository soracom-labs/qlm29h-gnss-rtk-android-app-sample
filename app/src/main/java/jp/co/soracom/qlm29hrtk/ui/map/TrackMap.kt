package jp.co.soracom.qlm29hrtk.ui.map

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackPointEntity
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun TrackMapCard(
    points: List<TrackPointEntity>,
    smartphonePoints: List<SmartphoneTrackPointEntity>,
    smartphoneVisible: Boolean,
    onSmartphoneVisibleChange: (Boolean) -> Unit,
    follow: Boolean,
    onFollowChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var enabledQualities by remember { mutableStateOf(QUALITY_COLORS.keys) }
    var selectedPoint by remember { mutableStateOf<TrackPointEntity?>(null) }
    Box(modifier) {
        val visiblePoints = points.filter { (it.quality.takeIf(QUALITY_COLORS::containsKey) ?: 0) in enabledQualities }
        MapLibreTrackView(
            points = visiblePoints,
            smartphonePoints = if (smartphoneVisible) smartphonePoints else emptyList(),
            follow = follow,
            onPointSelected = { id -> selectedPoint = points.firstOrNull { it.id == id } },
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            tonalElevation = 3.dp,
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                selectedPoint?.let { point ->
                    Text(
                        "${point.qualityLabel} · ${point.latitude}, ${point.longitude} · Alt ${point.altitude ?: "-"} m · Sat ${point.satellites ?: "-"} · HDOP ${point.hdop ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = smartphoneVisible,
                    onClick = { onSmartphoneVisibleChange(!smartphoneVisible) },
                    label = { Text("SP") },
                )
                QUALITY_LABELS.forEach { (quality, label) ->
                    FilterChip(
                        selected = quality in enabledQualities,
                        onClick = {
                            enabledQualities = if (quality in enabledQualities) enabledQualities - quality else enabledQualities + quality
                        },
                        label = { Text(label) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = follow, onCheckedChange = onFollowChange)
                    Text("Follow")
                }
            }
                QualityLegend(smartphoneVisible)
            }
        }
    }
}

@Composable
fun MapCacheControls() {
    val context = LocalContext.current
    var cacheStatus by remember { mutableStateOf("Limit: 200 MB") }
    var confirmClear by remember { mutableStateOf(false) }
    val cache = remember { MapCacheManager(context) }
    DisposableEffect(cache) {
        cache.applyLimit { error -> if (error != null) cacheStatus = "Cache error: $error" }
        onDispose { }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear map cache?") },
            text = { Text("Downloaded map data will be deleted and must be downloaded again when online.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    cacheStatus = "Clearing…"
                    cache.clear { error -> cacheStatus = error?.let { "Cache error: $it" } ?: "Cache cleared" }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Map cache", style = MaterialTheme.typography.titleMedium)
                Text(cacheStatus, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { confirmClear = true }) { Text("Clear") }
        }
    }
}

@Composable
private fun MapLibreTrackView(
    points: List<TrackPointEntity>,
    smartphonePoints: List<SmartphoneTrackPointEntity>,
    follow: Boolean,
    onPointSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val renderState = remember { MapRenderState() }
    DisposableEffect(mapView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.getMapAsync { ready ->
            ready.addOnMapClickListener { coordinate ->
                val screenPoint = ready.projection.toScreenLocation(coordinate)
                val feature = ready.queryRenderedFeatures(screenPoint, *QUALITY_COLORS.keys.map { "quality-$it" }.toTypedArray()).firstOrNull()
                feature?.getNumberProperty("point_id")?.toLong()?.let(onPointSelected)
                feature != null
            }
            ready.setStyle(STYLE_URL) { style ->
                installTrackLayers(style)
                map = ready
                updateTrack(ready, points, smartphonePoints, follow, renderState, force = true)
            }
        }
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map?.let { updateTrack(it, points, smartphonePoints, follow, renderState) } },
    )
}

private class MapRenderState {
    var points: List<TrackPointEntity>? = null
    var smartphonePoints: List<SmartphoneTrackPointEntity>? = null
    var followedSource: MapFollowTarget.Source? = null
    var followedTimestamp: Long = Long.MIN_VALUE
    var wasFollowing: Boolean = false
}

private fun installTrackLayers(style: org.maplibre.android.maps.Style) {
    if (style.getSource(SOURCE_SP_POINTS) == null) style.addSource(GeoJsonSource(SOURCE_SP_POINTS, FeatureCollection.fromFeatures(emptyArray())))
    if (style.getSource(SOURCE_SP_LINES) == null) style.addSource(GeoJsonSource(SOURCE_SP_LINES, FeatureCollection.fromFeatures(emptyArray())))
    if (style.getLayer(LAYER_SP_LINES) == null) {
        style.addLayer(
            LineLayer(LAYER_SP_LINES, SOURCE_SP_LINES).withProperties(
                PropertyFactory.lineColor(SP_LINE_COLOR),
                PropertyFactory.lineOpacity(0.6f),
                PropertyFactory.lineWidth(3f),
            ),
        )
    }
    if (style.getLayer(LAYER_SP_POINTS) == null) {
        style.addLayer(
            CircleLayer(LAYER_SP_POINTS, SOURCE_SP_POINTS).withProperties(
                PropertyFactory.circleColor(SP_POINT_COLOR),
                PropertyFactory.circleOpacity(0.7f),
                PropertyFactory.circleRadius(zoomedRadius(compact = 2.5, detailed = 5.0)),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(zoomedStroke(detailed = 1.0)),
            ),
        )
    }
    if (style.getSource(SOURCE_POINTS) == null) style.addSource(GeoJsonSource(SOURCE_POINTS, FeatureCollection.fromFeatures(emptyArray())))
    if (style.getSource(SOURCE_LINE) == null) style.addSource(GeoJsonSource(SOURCE_LINE, FeatureCollection.fromFeatures(emptyArray())))
    if (style.getLayer(LAYER_LINE) == null) {
        style.addLayer(LineLayer(LAYER_LINE, SOURCE_LINE).withProperties(PropertyFactory.lineColor("#464055"), PropertyFactory.lineWidth(3f)))
    }
    QUALITY_COLORS.forEach { (quality, color) ->
        val id = "quality-$quality"
        if (style.getLayer(id) == null) {
            style.addLayer(
                CircleLayer(id, SOURCE_POINTS)
                    .withFilter(Expression.eq(Expression.get("quality"), Expression.literal(quality)))
                    .withProperties(
                        PropertyFactory.circleColor(Color.parseColor(color)),
                        PropertyFactory.circleRadius(
                            zoomedRadius(compact = 2.5, detailed = if (quality == 4) 7.0 else 6.0),
                        ),
                        PropertyFactory.circleStrokeColor(Color.WHITE),
                        PropertyFactory.circleStrokeWidth(zoomedStroke(detailed = 2.0)),
                    ),
            )
        }
    }
    if (style.getLayer(LAYER_LATEST) == null) {
        style.addLayer(
            CircleLayer(LAYER_LATEST, SOURCE_POINTS)
                .withFilter(Expression.eq(Expression.get("is_latest"), Expression.literal(1)))
                .withProperties(
                    PropertyFactory.circleColor(Expression.get("display_color")),
                    PropertyFactory.circleRadius(zoomedRadius(compact = 4.0, detailed = 8.0)),
                    PropertyFactory.circleStrokeColor(Color.WHITE),
                    PropertyFactory.circleStrokeWidth(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.zoom(),
                            Expression.stop(12, 1.0),
                            Expression.stop(15, 1.0),
                            Expression.stop(17, 2.5),
                        ),
                    ),
                ),
        )
    }
}

private fun updateTrack(
    map: MapLibreMap,
    points: List<TrackPointEntity>,
    smartphonePoints: List<SmartphoneTrackPointEntity>,
    follow: Boolean,
    renderState: MapRenderState,
    force: Boolean = false,
) {
    val style = map.style ?: return
    if (force || renderState.points != points) {
        val ordered = points.asReversed()
        val latestId = points.firstOrNull()?.id
        val features = ordered.map { point ->
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                addNumberProperty("point_id", point.id)
                addNumberProperty("quality", point.quality.takeIf(QUALITY_COLORS::containsKey) ?: 0)
                addStringProperty("quality_label", point.qualityLabel)
                addNumberProperty("altitude", point.altitude)
                addNumberProperty("satellites", point.satellites)
                addNumberProperty("hdop", point.hdop)
                addNumberProperty("is_latest", if (point.id == latestId) 1 else 0)
                addStringProperty("display_color", QUALITY_COLORS[point.quality] ?: QUALITY_COLORS.getValue(0))
            }
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_POINTS)?.setGeoJson(FeatureCollection.fromFeatures(features))
        val coordinates = ordered.map { Point.fromLngLat(it.longitude, it.latitude) }
        val lineFeature = if (coordinates.size >= 2) Feature.fromGeometry(LineString.fromLngLats(coordinates)) else null
        style.getSourceAs<GeoJsonSource>(SOURCE_LINE)?.setGeoJson(
            lineFeature?.let { FeatureCollection.fromFeature(it) } ?: FeatureCollection.fromFeatures(emptyArray()),
        )
        renderState.points = points
    }
    if (force || renderState.smartphonePoints != smartphonePoints) {
        val orderedSp = smartphonePoints.asReversed()
        val spFeatures = orderedSp.map { point ->
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                addNumberProperty("point_id", point.id)
                addNumberProperty("accuracy", point.accuracy)
            }
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_SP_POINTS)?.setGeoJson(FeatureCollection.fromFeatures(spFeatures))
        val spLineFeatures = orderedSp.groupBy { it.segmentId }.values.mapNotNull { segment ->
            val segmentCoordinates = segment.map { Point.fromLngLat(it.longitude, it.latitude) }
            if (segmentCoordinates.size >= 2) Feature.fromGeometry(LineString.fromLngLats(segmentCoordinates)) else null
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_SP_LINES)?.setGeoJson(FeatureCollection.fromFeatures(spLineFeatures))
        renderState.smartphonePoints = smartphonePoints
    }
    val qlmTarget = points.firstOrNull()?.let {
        MapFollowTarget(MapFollowTarget.Source.QLM, it.timestamp, it.latitude, it.longitude)
    }
    val smartphoneTarget = smartphonePoints.firstOrNull()?.let {
        MapFollowTarget(MapFollowTarget.Source.SMARTPHONE, it.timestamp, it.latitude, it.longitude)
    }
    val target = MapViewportPolicy.chooseTarget(qlmTarget, smartphoneTarget)
    if (MapViewportPolicy.shouldMove(
            followEnabled = follow,
            wasFollowing = renderState.wasFollowing,
            previousSource = renderState.followedSource,
            previousTimestamp = renderState.followedTimestamp,
            target = target,
            force = force,
        )
    ) {
        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(target!!.latitude, target.longitude))
            .zoom(map.cameraPosition.zoom.coerceAtLeast(16.0))
            .build()
        renderState.followedSource = target.source
        renderState.followedTimestamp = target.timestamp
    }
    renderState.wasFollowing = follow
}

@Composable
private fun QualityLegend(smartphoneVisible: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (smartphoneVisible) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.background(ComposeColor(Color.parseColor(SP_POINT_COLOR)), CircleShape).padding(5.dp))
                Text("SP", style = MaterialTheme.typography.labelSmall)
            }
        }
        QUALITY_LABELS.forEach { (quality, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.background(ComposeColor(Color.parseColor(QUALITY_COLORS.getValue(quality))), CircleShape).padding(5.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private class MapCacheManager(context: Context) {
    private val manager = OfflineManager.getInstance(context.applicationContext)
    fun applyLimit(done: (String?) -> Unit) = manager.setMaximumAmbientCacheSize(CACHE_LIMIT_BYTES, callback(done))
    fun clear(done: (String?) -> Unit) = manager.clearAmbientCache(callback(done))
    private fun callback(done: (String?) -> Unit) = object : OfflineManager.FileSourceCallback {
        override fun onSuccess() = done(null)
        override fun onError(message: String) = done(message)
    }
}
