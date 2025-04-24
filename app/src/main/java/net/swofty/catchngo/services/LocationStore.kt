// ────────────────────────────────────────────────────────────────────────────────
// LocationStore.kt  –  single source of truth for the latest GPS fix
// ────────────────────────────────────────────────────────────────────────────────
package net.swofty.catchngo.services

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocationStore {
    /** Most-recent fix; null until first update arrives */
    private val _latest = MutableStateFlow<Location?>(null)
    val latest          = _latest.asStateFlow()

    fun update(loc: Location) { _latest.value = loc }
}
