package com.deadzone.messenger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.deadzone.messenger.model.MeshMessage

class MeshService : Service() {
    private val binder = LocalBinder()
    lateinit var meshManager: MeshNetworkManager
        private set

    inner class LocalBinder : Binder() {
        fun getService(): MeshService = this@MeshService
    }

    private val serviceScopeJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceScopeJob)

    override fun onCreate() {
        super.onCreate()
        meshManager = MeshNetworkManager(this)
        startForegroundService()
        
        serviceScope.launch {
            meshManager.messages.collect { msg ->
                if (!msg.isFromMe) {
                    showMessageNotification(msg)
                }
            }
        }
    }

    private fun showMessageNotification(message: MeshMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val route = if (message.chatGroupId == "GROUP") "chat/group" else "chat/device/${message.chatGroupId}"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_route", route)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            message.id.hashCode(), 
            intent, 
            flags
        )
        
        val builder = NotificationCompat.Builder(this, "mesh_service_channel")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("New Message from ${message.senderName}")
            .setContentText(message.content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(message.id.hashCode(), builder.build())
    }

    private fun startForegroundService() {
        val channelId = "mesh_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Background Connection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Deadzone Messenger")
            .setContentText("Running in the background to sync messages")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stopMesh()
        meshManager.destroy()
        serviceScopeJob.cancel() // Prevent coroutine leaks
    }
}
