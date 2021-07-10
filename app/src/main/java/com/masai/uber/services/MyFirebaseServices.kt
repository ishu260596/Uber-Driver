package com.masai.uber.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.masai.uber.model.eventbus.DriverRequestReceived
import com.masai.uber.utlis.*
import org.greenrobot.eventbus.EventBus
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

        if (dataRec[NOTIFICATION_TITLE].equals(REQUEST_DRIVER_TITLE)) {

            EventBus.getDefault().postSticky(
                DriverRequestReceived(
                    dataRec.get(RIDER_KEY),
                    dataRec.get(RIDER_PICKUP_LOCATION)
                )
            )

        } else {
            Common.showNotification(
                this, Random.nextInt(),
                dataRec[NOTIFICATION_TITLE],
                dataRec[NOTIFICATION_CONTENT],
                null
            )
        }

    }

}