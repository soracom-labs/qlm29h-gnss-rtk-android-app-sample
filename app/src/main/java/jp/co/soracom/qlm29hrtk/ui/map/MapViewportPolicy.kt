package jp.co.soracom.qlm29hrtk.ui.map

/** A map target without a dependency on MapLibre, suitable for unit tests. */
data class MapFollowTarget(
    val source: Source,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
) {
    enum class Source { QLM, SMARTPHONE }
}

/**
 * QLM is authoritative; Smartphone GNSS is a visual fallback only. Camera
 * updates are accepted only when that target advances, preventing independent
 * SP emissions from making the map briefly jump. See MAP-01 and MAP-02.
 */
object MapViewportPolicy {
    fun chooseTarget(qlm: MapFollowTarget?, smartphone: MapFollowTarget?): MapFollowTarget? = qlm ?: smartphone

    fun shouldMove(
        followEnabled: Boolean,
        wasFollowing: Boolean,
        previousSource: MapFollowTarget.Source?,
        previousTimestamp: Long,
        target: MapFollowTarget?,
        force: Boolean = false,
    ): Boolean = followEnabled && target != null && (
        force || !wasFollowing || target.source != previousSource || target.timestamp > previousTimestamp
    )
}
