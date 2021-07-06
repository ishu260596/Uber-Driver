package com.masai.uber.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.masai.uber.utlis.Common
import com.masai.uber.utlis.NOTIFICATION_CONTENT
import com.masai.uber.utlis.NOTIFICATION_TITLE
import com.masai.uber.utlis.UserUtils
import kotlin.random.Random

class MyFirebaseServices : FirebaseMessagingService() {

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        if (FirebaseAuth.getInstance().currentUser != null) {
            UserUtils.updateToken(this, s)
        }
    }

    override fun onMessageReceived(s: RemoteMessage) {
        super.onMessageReceived(s)
        val dataRec: Map<String, String> = s.data

        Common.showNotification(
            this, Random.nextInt(),
            dataRec[NOTIFICATION_TITLE],
            dataRec[NOTIFICATION_CONTENT],
            null
        )
    }


}