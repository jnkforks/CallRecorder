package com.redridgeapps.callrecorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.redridgeapps.callrecorder.MainActivity
import com.redridgeapps.callrecorder.R
import com.redridgeapps.callrecorder.callutils.CallRecorder
import com.redridgeapps.callrecorder.callutils.CallStatus.*
import com.redridgeapps.callrecorder.callutils.CallStatusListener
import com.redridgeapps.callrecorder.utils.NOTIFICATION_CALL_SERVICE_ID
import com.redridgeapps.callrecorder.utils.prefs.Prefs
import dagger.android.AndroidInjection
import kotlinx.coroutines.launch
import javax.inject.Inject

class CallingService : LifecycleService() {

    @Inject
    lateinit var prefs: Prefs

    @Inject
    lateinit var telephonyManager: TelephonyManager

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var callRecorder: CallRecorder

    private val callStatusListener = createCallStatusListener()

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()

        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()

            return START_NOT_STICKY
        }

        createNotification()

        telephonyManager.listen(callStatusListener, PhoneStateListener.LISTEN_CALL_STATE)

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        telephonyManager.listen(callStatusListener, PhoneStateListener.LISTEN_NONE)
        callRecorder.releaseRecorder()
    }

    private fun createCallStatusListener() =
        CallStatusListener { status, phoneNumber, callDirection ->
            when (status) {
                MissedCall, IncomingCallReceived -> Unit
                IncomingCallAnswered, OutgoingCallStarted -> lifecycleScope.launch { callRecorder.startRecording() }
                IncomingCallEnded, OutgoingCallEnded -> callRecorder.stopRecording(
                    phoneNumber,
                    callDirection
                )
            }
        }

    private fun createNotification() {

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, createNotificationChannel())
            .setContentText("Standing by to record...")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setShowWhen(false)
            .build()

        startForeground(NOTIFICATION_CALL_SERVICE_ID, notification)
    }

    private fun createNotificationChannel(): String {

        val channel = NotificationChannel(
            javaClass.simpleName,
            "Call Recorder",
            NotificationManager.IMPORTANCE_LOW
        )

        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET

        notificationManager.createNotificationChannel(channel)

        return channel.id
    }

    companion object {

        private const val ACTION_STOP = "ACTION_STOP"

        fun start(context: Context) {

            ContextCompat.startForegroundService(
                context,
                Intent(context, CallingService::class.java)
            )
        }

        fun stop(context: Context) {

            val intent = Intent(context, CallingService::class.java)
            intent.action = ACTION_STOP

            ContextCompat.startForegroundService(context, intent)
        }
    }
}
