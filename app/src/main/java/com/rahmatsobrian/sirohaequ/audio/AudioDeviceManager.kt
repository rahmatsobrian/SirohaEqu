package com.rahmatsobrian.sirohaequ.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.rahmatsobrian.sirohaequ.data.model.DeviceCategory
import com.rahmatsobrian.sirohaequ.data.model.DeviceIdentity
import com.rahmatsobrian.sirohaequ.data.model.DeviceProfile
import com.rahmatsobrian.sirohaequ.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveDeviceInfo(
    val name: String,
    val androidType: Int,
    val isBluetooth: Boolean,
    val isSink: Boolean, // output device
    val sampleRates: List<Int>,
    val channelCounts: List<Int>,
    val encodings: List<Int>
)

/**
 * Watches [AudioManager] for output-device changes via [AudioDeviceCallback]
 * (API 23+, always available given minSdk 29) and exposes the currently
 * active output device as a [StateFlow].
 *
 * Device *names* on Android are best-effort: AudioDeviceInfo.getProductName()
 * is frequently generic ("Bluetooth", "Headset") depending on OEM/firmware.
 * We therefore never claim exact hardware identification — matching against
 * saved [DeviceProfile]s is a heuristic (name + type + bluetooth-ness), and
 * the UI always offers manual profile selection as a fallback when the
 * heuristic is uncertain or wrong.
 */
class AudioDeviceManager(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _activeDevice = MutableStateFlow<ActiveDeviceInfo?>(null)
    val activeDevice: StateFlow<ActiveDeviceInfo?> = _activeDevice.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshActiveDevice()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refreshActiveDevice()
        }
    }

    fun startWatching() {
        try {
            audioManager.registerAudioDeviceCallback(callback, null)
            refreshActiveDevice()
        } catch (e: RuntimeException) {
            AppLogger.log("AudioDeviceManager", "startWatching failed: ${e.message}")
        }
    }

    fun stopWatching() {
        try {
            audioManager.unregisterAudioDeviceCallback(callback)
        } catch (_: RuntimeException) {
        }
    }

    private fun refreshActiveDevice() {
        try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            // Heuristic: prefer the most recently added non-default (external) sink;
            // Android does not expose a direct "currently routed" API without an
            // active AudioTrack, so this is the best generally-available signal.
            val preferred = outputs.firstOrNull { it.isExternal() } ?: outputs.firstOrNull()
            _activeDevice.value = preferred?.toActiveDeviceInfo()
        } catch (e: RuntimeException) {
            AppLogger.log("AudioDeviceManager", "refreshActiveDevice failed: ${e.message}")
            _activeDevice.value = null
        }
    }

    private fun AudioDeviceInfo.isExternal(): Boolean = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_DOCK -> true
        else -> false
    }

    private fun AudioDeviceInfo.toActiveDeviceInfo(): ActiveDeviceInfo = ActiveDeviceInfo(
        name = productName?.toString()?.ifBlank { null } ?: fallbackNameFor(type),
        androidType = type,
        isBluetooth = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        isSink = isSink,
        sampleRates = sampleRates?.toList() ?: emptyList(),
        channelCounts = channelCounts?.toList() ?: emptyList(),
        encodings = encodings?.toList() ?: emptyList()
    )

    private fun fallbackNameFor(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Audio Device"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio Device"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
        else -> "Unknown Audio Device"
    }

    companion object {
        fun categoryFor(type: Int): DeviceCategory = when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceCategory.BLUETOOTH
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> DeviceCategory.HEADSET
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> DeviceCategory.HEADPHONE
            AudioDeviceInfo.TYPE_USB_HEADSET -> DeviceCategory.USB_DAC
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> DeviceCategory.USB_DAC
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> DeviceCategory.SPEAKER
            else -> DeviceCategory.OTHER
        }

        /**
         * Best-effort match: exact product-name match wins; otherwise falls back
         * to "same android type + same bluetooth-ness" if there's exactly one
         * such profile (avoids silently picking the wrong one of several).
         */
        fun matchProfile(active: ActiveDeviceInfo, profiles: List<DeviceProfile>): DeviceProfile? {
            val byName = profiles.firstOrNull {
                it.identity?.productName?.equals(active.name, ignoreCase = true) == true
            }
            if (byName != null) return byName

            val byTypeCandidates = profiles.filter {
                it.identity?.androidDeviceType == active.androidType &&
                    it.identity.isBluetooth == active.isBluetooth
            }
            return byTypeCandidates.singleOrNull()
        }
    }
}
