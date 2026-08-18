package com.rahmatsobrian.sirohaequ.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceCategory {
    IEM, EARPHONE, HEADPHONE, HEADSET, TWS, BLUETOOTH, USB_DAC, SPEAKER, OTHER
}

/**
 * Identity used to match a currently-connected AudioDeviceInfo back to a saved
 * profile. Android does not guarantee a stable unique ID for external audio
 * devices across reconnects on all OEMs/API levels, so matching is done on a
 * best-effort (name + type) basis, with the profile also selectable manually
 * as a fallback when auto-matching fails or is ambiguous.
 */
@Serializable
data class DeviceIdentity(
    val productName: String,
    val androidDeviceType: Int, // AudioDeviceInfo.TYPE_*
    val isBluetooth: Boolean
)

@Serializable
data class DeviceProfile(
    val id: String,
    val displayName: String,
    val category: DeviceCategory,
    val identity: DeviceIdentity? = null, // null = manual-only profile, never auto-matched
    val presetId: String,
    val chainOverride: ProcessingChain? = null, // null = use preset's own chain
    val channelConfigLabel: String = "Stereo",
    val isAutoApply: Boolean = true,
    val createdAtEpochMs: Long = 0L
)
