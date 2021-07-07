package com.masai.uber.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.masai.uber.R
import com.masai.uber.databinding.ActivityPhoneVerificationBinding
import com.masai.uber.ui.fragments.PasswordFragment
import com.masai.uber.utlis.KEY_DRIVER_MOBILE_NUMBER
import com.masai.uber.utlis.PreferenceHelper
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType
import java.util.concurrent.TimeUnit


class PhoneVerificationActivity : AppCompatActivity() {
    private var binding: ActivityPhoneVerificationBinding? = null

    private lateinit var mAuth: FirebaseAuth
    private var mVerificationId: String? = null

    private lateinit var mobileNumber: String

    private var TIMER: Int = 30

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneVerificationBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        PreferenceHelper.getSharedPreferences(this)

        mAuth = FirebaseAuth.getInstance()
        if (intent != null && intent.extras != null) {
            mobileNumber = "+91${intent.getStringExtra("number")!!}"
            binding!!.tvMobile.text = "+91 ${intent.getStringExtra("number")!!}"
        }

        object : CountDownTimer(31000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val counter =
                    "Resend code in : ${java.lang.String.valueOf(TIMER).toString()} sec"
                binding!!.tvResendCode.text = counter
                TIMER--
            }

            override fun onFinish() {
                Toast.makeText(
                    this@PhoneVerificationActivity,
                    "wait sending you the verification code again !", Toast.LENGTH_SHORT
                ).show()
                binding!!.tvResendCode.text =
                    "Resend code in : ${30} sec"
            }
        }.start()

        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(mobileNumber)       // Phone number to verify
            .setTimeout(100L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)


        binding!!.btnNextOtp.setOnClickListener {
            if (!binding!!.otpView.text.isNullOrEmpty()) {
                verifyVerificationCode(binding!!.otpView.text.toString())
            }
        }

    }

    private val callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                Log.d("tag", "onVerificationCompleted:$phoneAuthCredential")
                binding!!.otpView.setText(phoneAuthCredential.smsCode.toString())
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(this@PhoneVerificationActivity, e.message, Toast.LENGTH_LONG)
                        .show()
                } else if (e is FirebaseTooManyRequestsException) {
                    Toast.makeText(this@PhoneVerificationActivity, e.message, Toast.LENGTH_LONG)
                        .show()
                }
            }

            override fun onCodeSent(
                s: String,
                forceResendingToken: PhoneAuthProvider.ForceResendingToken
            ) {
                super.onCodeSent(s, forceResendingToken)
                mVerificationId = s
            }
        }

    private fun verifyVerificationCode(code: String) {
        //creating the credential
        val credential = mVerificationId?.let { PhoneAuthProvider.getCredential(it, code) }
        //signing the user
        if (credential != null) {
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
//                    AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.SUCCESS)
//                        .setTitle("Success")
//                        .show()
                    redirect()
                } else {
//                    AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.ERROR)
//                        .setTitle("Failed")
//                        .show()
                }
            }
    }

    private fun redirect() {
            PreferenceHelper.writeStringToPreference(KEY_DRIVER_MOBILE_NUMBER, mobileNumber)
        startActivity(
            Intent(this, LaunchFragmentsActivity::class.java)

        )
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}