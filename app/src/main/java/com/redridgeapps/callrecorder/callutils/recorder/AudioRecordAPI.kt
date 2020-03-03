package com.redridgeapps.callrecorder.callutils.recorder

import android.content.SharedPreferences
import android.media.AudioRecord
import android.media.MediaRecorder
import com.redridgeapps.callrecorder.utils.PREF_AUDIO_RECORD_AUDIO_ENCODING
import com.redridgeapps.callrecorder.utils.PREF_AUDIO_RECORD_CHANNELS
import com.redridgeapps.callrecorder.utils.PREF_AUDIO_RECORD_SAMPLE_RATE
import com.redridgeapps.callrecorder.utils.get
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecordAPI(
    private val saveDir: File,
    private val prefs: SharedPreferences
) : Recorder {

    private val saveFileExt = ".pcm"
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var savePath: String? = null

    override fun startRecording(fileName: String) {

        val sampleRate = prefs.get(PREF_AUDIO_RECORD_SAMPLE_RATE)
        val channels = prefs.get(PREF_AUDIO_RECORD_CHANNELS)
        val audioEncoding = prefs.get(PREF_AUDIO_RECORD_AUDIO_ENCODING)

        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channels, audioEncoding)

        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_CALL,
            sampleRate,
            channels,
            audioEncoding,
            bufferSize
        )

        recorder!!.startRecording()
        isRecording = true
        recordingThread =
            Thread(Runnable { writeAudioDataToFile(fileName, bufferSize) }, "AudioRecorder Thread")
        recordingThread!!.start()
    }

    override fun stopRecording(): String {
        if (recorder != null) {
            isRecording = false
            recorder!!.stop()
            recorder!!.release()
            recorder = null
            recordingThread = null
        }

        return savePath ?: error("savePath is null")
    }

    override fun releaseRecorder() {
        recorder?.release()
        recorder = null
    }

    private fun writeAudioDataToFile(fileName: String, bufferSize: Int) {

        val fileNameWithExt = fileName + saveFileExt
        val newSavePath = File(saveDir, fileNameWithExt)
        savePath = newSavePath.absolutePath

        val dos = DataOutputStream(BufferedOutputStream(FileOutputStream(newSavePath)))
        val shortBuffer = ShortArray(bufferSize / 2)

        while (isRecording) {
            recorder!!.read(shortBuffer, 0, shortBuffer.size)

            val byteBuffer = shortBuffer.to2ByteArray()
            dos.write(byteBuffer)
        }

        dos.close()
    }

    private fun ShortArray.to2ByteArray(): ByteArray {

        val resultBytes = ByteArray(size * 2)
        val byteBuffer = ByteBuffer.wrap(resultBytes).order(ByteOrder.LITTLE_ENDIAN)

        map { byteBuffer.putShort(it) }

        return resultBytes
    }
}
