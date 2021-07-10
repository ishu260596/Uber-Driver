package com.masai.uber.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.masai.uber.R
import com.masai.uber.utlis.UserUtils
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    var handler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        mAuth = FirebaseAuth.getInstance()
        handler = Handler()
        val user = mAuth.currentUser
        if (user != null) {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.ERROR)
                            .setTitle("Failed")
                            .show()
                        return@OnCompleteListener
                    }
                    // Get new FCM registration token
                    val token = task.result
                    Log.d("Token", token.toString())
                    UserUtils.updateToken(this, token)
                })
        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            redirect("A")
        } else {
            redirect("B")
        }

    }

    private fun redirect(s: String) {

        if (s == "A") {
            handler!!.postDelayed({
                startActivity(Intent(this, DriverHomeActivity::class.java))
                finish()
            }, 4000)
        } else {
            handler!!.postDelayed({
                startActivity(Intent(this, GetStartedActivity::class.java))
                finish()
            }, 4000)
        }
    }
}