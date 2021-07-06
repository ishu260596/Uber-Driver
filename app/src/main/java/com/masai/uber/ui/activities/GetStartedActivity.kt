package com.masai.uber.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.masai.uber.databinding.ActivityGetStartedBinding
import com.masai.uber.utlis.UserUtils
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType

class GetStartedActivity : AppCompatActivity() {
    private var binding: ActivityGetStartedBinding? = null
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        val view = binding!!.root
        setContentView(view)
        mAuth = FirebaseAuth.getInstance()

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

        binding!!.btnGetStarted.setOnClickListener {
            if (mAuth.currentUser != null) {
                startActivity(Intent(this, DriverHomeActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}