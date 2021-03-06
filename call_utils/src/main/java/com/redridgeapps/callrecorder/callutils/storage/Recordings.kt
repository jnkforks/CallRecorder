package com.redridgeapps.callrecorder.callutils.storage

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import com.redridgeapps.callrecorder.callutils.db.Recording
import com.redridgeapps.callrecorder.callutils.db.RecordingQueries
import com.redridgeapps.callrecorder.callutils.recording.RecordingJob
import com.redridgeapps.callrecorder.common.StartupInitializer
import com.redridgeapps.callrecorder.common.utils.extension
import com.redridgeapps.callrecorder.common.utils.launchUnit
import com.redridgeapps.callrecorder.common.utils.replaceExtension
import com.redridgeapps.callrecorder.prefs.PREF_RECORDINGS_STORAGE_PATH
import com.redridgeapps.callrecorder.prefs.Prefs
import com.redridgeapps.mp3encoder.EncodingJob
import com.redridgeapps.mp3encoder.Mp3Encoder
import com.redridgeapps.wavutils.WavData
import com.redridgeapps.wavutils.WavFileUtils
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.READ
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Recordings @Inject constructor(
    private val recordingQueries: RecordingQueries,
    private val contactNameFetcher: ContactNameFetcher
) {

    suspend fun saveRecording(recordingJob: RecordingJob) = withContext(Dispatchers.IO) {

        val phoneNumber = recordingJob.newCallEvent.phoneNumber
        val name = contactNameFetcher.getContactName(phoneNumber) ?: "Unknown ($phoneNumber)"

        val duration = FileChannel.open(recordingJob.savePath.toAbsolutePath(), READ)
            .use { WavFileUtils.calculateDuration(it) }

        recordingQueries.insert(
            name = name,
            number = phoneNumber,
            start_instant = recordingJob.recordingStartInstant,
            duration = duration,
            call_direction = recordingJob.newCallEvent.callDirection,
            save_path = recordingJob.savePath.toAbsolutePath().toString(),
            save_format = recordingJob.savePath.extension
        )
    }

    fun getRecording(recordingId: Long): Flow<Recording> {
        return recordingQueries.get(listOf(recordingId)).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getRecordingList(): Flow<List<Recording>> {
        return recordingQueries.getAll().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun trimSilenceEnds(recordingId: Long) = withContext(Dispatchers.IO) {

        val recording = getRecording(recordingId).first()
        val recordingPath = Paths.get(recording.save_path)
        val outputPath = recordingPath.replaceExtension("trimmed.wav")

        WavFileUtils.trimSilenceEnds(recordingPath, outputPath)

        // Replace original file with trimmed file
        Files.delete(recordingPath)
        Files.move(outputPath, recordingPath)

        // Update duration
        val duration =
            FileChannel.open(recordingPath, READ).use { WavFileUtils.calculateDuration(it) }
        recordingQueries.updateDuration(duration, recordingId)
    }

    suspend fun convertToMp3(recordingId: Long) = withContext(Dispatchers.IO) {

        val recording = getRecording(recordingId).first()
        val recordingPath = Paths.get(recording.save_path)
        val wavData = getWavData(recordingId)
        val outputPath = recordingPath.replaceExtension("mp3")

        val encodingJob = EncodingJob(
            wavData = wavData,
            wavPath = recordingPath,
            mp3Path = outputPath
        )

        Mp3Encoder.encode(encodingJob)
    }

    suspend fun deleteRecording(recordingIdList: List<Long>) = withContext(Dispatchers.IO) {
        val recordings = recordingQueries.get(recordingIdList).executeAsList()

        recordings.forEach {
            Files.delete(Paths.get(it.save_path))
        }

        recordingQueries.delete(recordingIdList)
    }

    fun getIsStarred(recordingId: Long): Flow<Boolean> {
        return recordingQueries.getIsStarred(recordingId).asFlow().mapToOne()
    }

    suspend fun toggleStar(recordingIdList: List<Long>) = withContext(Dispatchers.IO) {
        recordingQueries.toggleStar(recordingIdList)
    }

    fun getSkipAutoDelete(recordingId: Long): Flow<Boolean> {
        return recordingQueries.getSkipAutoDelete(recordingId).asFlow().mapToOne()
    }

    suspend fun toggleSkipAutoDelete(recordingIdList: List<Long>) = withContext(Dispatchers.IO) {
        recordingQueries.toggleSkipAutoDelete(recordingIdList)
    }

    suspend fun updateContactNames() = withContext(Dispatchers.IO) {

        val recordings = recordingQueries.getAll().executeAsList()

        recordings.distinctBy { it.number }.forEach { recording ->
            val name = contactNameFetcher.getContactName(recording.number) ?: recording.name
            recordingQueries.updateContactName(name, recording.number)
        }
    }

    suspend fun getWavData(recordingId: Long): WavData = withContext(Dispatchers.IO) {

        val recording = getRecording(recordingId).first()
        val recordingPath = Paths.get(recording.save_path)
        val fileChannel = FileChannel.open(recordingPath, READ)

        return@withContext fileChannel.use { WavFileUtils.readWavData(fileChannel) }
    }

    internal suspend fun deleteOverDaysOldIfNotSkippedAutoDelete(
        duration: Duration
    ) = withContext(Dispatchers.IO) {
        val days = duration.toDays()
        recordingQueries.deleteOverDaysOldIfNotSkippedAutoDelete(days.toString())
    }

    companion object {

        internal fun generateFilePath(saveDir: String, fileName: String): Path {
            val fileNameWithExt = "$fileName.wav"
            return Paths.get(saveDir).resolve(fileNameWithExt)
        }

        fun getRecordingsStoragePath(context: Context): Path {

            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED)
                error("External storage is not writable")

            val externalStorageVolumes = ContextCompat.getExternalFilesDirs(context, null)
            val primaryExternalStorage = externalStorageVolumes[0]
            val savePath = Paths.get(primaryExternalStorage.path, "CallRecordings")

            if (!Files.exists(savePath))
                Files.createDirectory(savePath)

            return savePath
        }
    }
}

class RecordingStoragePathInitializer @Inject constructor(
    val prefs: Prefs
) : StartupInitializer {

    override fun initialize(context: Context) = GlobalScope.launchUnit {

        if (prefs.prefStringOrNull(PREF_RECORDINGS_STORAGE_PATH).first() == null) {

            val newRecordingPath = Recordings.getRecordingsStoragePath(context).toString()

            prefs.editor { setString(PREF_RECORDINGS_STORAGE_PATH, newRecordingPath) }
        }
    }
}
