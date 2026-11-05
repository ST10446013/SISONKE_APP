package com.example.sisonke_app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sisonke_app.R
import com.example.sisonke_app.dashboard.DashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SisonkeMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences("sisonke_prefs", Context.MODE_PRIVATE)
            .edit().putString("pending_fcm_token", token).apply()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Sisonke community alert"
        val body = message.notification?.body ?: message.data["body"] ?: "There is a new update in your neighbourhood."
        saveNotificationToHistory(title, body)
        showNotification(title, body)
    }

    private fun saveNotificationToHistory(title: String, body: String) {
        val prefs = getSharedPreferences("sisonke_prefs", MODE_PRIVATE)
        val existingJson = prefs.getString("notification_history", "[]") ?: "[]"
        val gson = Gson()
        val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
        val list: MutableList<NotificationItem> = try {
            gson.fromJson(existingJson, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
        list.add(0, NotificationItem(title, body, System.currentTimeMillis()))
        if (list.size > 50) {
            list.subList(50, list.size).clear()
        }
        prefs.edit().putString("notification_history", gson.toJson(list)).apply()
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "sisonke_community_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Community safety alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val intent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bell_white)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
            // Notifications are disabled by the user or Android permission is not granted.
        }
    }
}
