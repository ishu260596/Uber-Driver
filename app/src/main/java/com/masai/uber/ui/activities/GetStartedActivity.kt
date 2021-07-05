package com.masai.uber.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.masai.uber.databinding.ActivityGetStartedBinding

class GetStartedActivity : AppCompatActivity() {
    private var binding: ActivityGetStartedBinding? = null
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        val view = binding!!.root
        setContentView(view)
        mAuth = FirebaseAuth.getInstance()

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