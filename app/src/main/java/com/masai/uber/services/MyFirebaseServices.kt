package com.masai.uber.services

import android.util.Log
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
        val dataRec = s.data
        Log.d("tag", "in firebase ${s.data.toString()}")


        EventBus.getDefault().postSticky(
            DriverRequestReceived(
                dataRec[RIDER_KEY],
                dataRec[RIDER_PICKUP_LOCATION],
                dataRec[START_ADDRESS],
                dataRec[END_ADDRESS]
            )
        )


        Common.showNotification(
            this, Random.nextInt(),
            dataRec[NOTIFICATION_TITLE],
            dataRec[NOTIFICATION_CONTENT],
            null
        , DriverRequestReceived(
                dataRec[RIDER_KEY],
                dataRec[RIDER_PICKUP_LOCATION],
                dataRec[START_ADDRESS],
                dataRec[END_ADDRESS]
            )
        )
    }
}

