package com.redridgeapps.callrecorder.callutils

import android.media.AudioFormat
import com.redridgeapps.repository.callutils.PcmChannels
import com.redridgeapps.repository.callutils.PcmEncoding

fun PcmChannels.toAudioRecordChannels(): Int = when (this) {
    PcmChannels.MONO -> AudioFormat.CHANNEL_IN_MONO
    PcmChannels.STEREO -> AudioFormat.CHANNEL_IN_STEREO
}

fun PcmEncoding.toAudioRecordEncoding(): Int = when (this) {
    PcmEncoding.PCM_8BIT -> AudioFormat.ENCODING_PCM_8BIT
    PcmEncoding.PCM_16BIT -> AudioFormat.ENCODING_PCM_16BIT
    PcmEncoding.PCM_FLOAT -> AudioFormat.ENCODING_PCM_FLOAT
}