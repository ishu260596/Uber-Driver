package com.masai.uber.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.masai.uber.R
import com.masai.uber.ui.fragments.PasswordFragment

class LaunchFragmentsActivity : AppCompatActivity() {
    private lateinit var mobileNumber: String
    private lateinit var fragmentManager: FragmentManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_fragments)
        if (intent != null && intent.extras != null) {
            mobileNumber = intent.getStringExtra("mobile").toString()
        }
        launchFragment()
    }

    private fun launchFragment() {
        val passwordFragment = PasswordFragment()
        val bundle = Bundle()
        bundle.putString("mobile", mobileNumber)
        fragmentManager = supportFragmentManager
        passwordFragment.arguments = bundle
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container3, passwordFragment)
        transaction.addToBackStack("FragmentPassword")
        transaction.commit()
    }
}