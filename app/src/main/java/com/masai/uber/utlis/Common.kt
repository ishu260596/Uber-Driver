package com.masai.uber.utlis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import com.masai.uber.R
import com.masai.uber.services.MyFirebaseServices

object Common {
    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        body: String?,
        intent: Intent?
    ) {

        var pendingIntent: PendingIntent? = null
//
//            if (intent != null) {
        pendingIntent = PendingIntent.getActivity(
            context,
            id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val NOTIFICATION_CHANNEL_ID = "ishu.masai.school"
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel: NotificationChannel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Uber", NotificationManager.IMPORTANCE_HIGH
                )
            notificationChannel.description = "Hi there"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = Color.YELLOW

            notificationManager.createNotificationChannel(notificationChannel)

        }
        val builder: NotificationCompat.Builder = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_ID)

        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car)
            .setLargeIcon(
                BitmapFactory
                    .decodeResource(
                        context.resources,
                        R.drawable.ic_baseline_directions_car
                    )
            )

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        val notification = builder.build()

        notificationManager.notify(id, notification)

//            }
    }

    //DECODE POLY
    fun decodePoly(encoded: String): java.util.ArrayList<LatLng?> {
        val poly = java.util.ArrayList<LatLng?>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

}