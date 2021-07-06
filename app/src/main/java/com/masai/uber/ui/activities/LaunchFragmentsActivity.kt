package com.masai.uber.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.masai.uber.R
import com.masai.uber.ui.fragments.PasswordFragment

class LaunchFragmentsActivity : AppCompatActivity() {
    private lateinit var fragmentManager: FragmentManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_fragments)
        launchFragment()
    }

    private fun launchFragment() {
        val passwordFragment = PasswordFragment()
        fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container3, passwordFragment)
        transaction.addToBackStack("FragmentPassword")
        transaction.commit()
    }
}