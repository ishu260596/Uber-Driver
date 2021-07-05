package com.masai.uber.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.cazaea.sweetalert.SweetAlertDialog
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.masai.uber.R
import com.masai.uber.databinding.ActivityNavDriverHomeBinding
import com.masai.uber.utlis.KEY_DRIVER_DISPLAY_NAME
import com.masai.uber.utlis.KEY_DRIVER_PROFILE_URL
import com.masai.uber.utlis.PreferenceHelper
import de.hdodenhof.circleimageview.CircleImageView


class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityNavDriverHomeBinding

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNavDriverHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PreferenceHelper.getSharedPreferences(this)
        setSupportActionBar(binding.appBarNavMain.toolbar)

        drawerLayout = binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_nav_main_driver)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.driver_home), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.signOut) {

                val sDialog =
                    SweetAlertDialog(this@DriverHomeActivity, SweetAlertDialog.WARNING_TYPE)
                sDialog.titleText = "Sign out"
                sDialog.contentText = "Are you sure!"
                sDialog.cancelText = "No"
                sDialog.confirmText = "Yes"
                sDialog.showCancelButton(true)
                sDialog.progressHelper.barColor = Color.parseColor("#CF7351")
                sDialog.setCancelable(true)
                sDialog.setCancelClickListener { sDialog.cancel(); }
                sDialog.setConfirmClickListener {
                    FirebaseAuth.getInstance().signOut()
                    redirect()
                }
                sDialog.show()
            }
            true
        }

        //setData
        val view: View = navView.getHeaderView(0)
        val tvName = view.findViewById<TextView>(R.id.tvNameHeader)
        val ivImage = view.findViewById<CircleImageView>(R.id.profile_image_header)
        val tvRating = view.findViewById<TextView>(R.id.tvRating)
        val name = PreferenceHelper.getStringFromPreference(KEY_DRIVER_DISPLAY_NAME).toString()
        val url = PreferenceHelper.getStringFromPreference(KEY_DRIVER_PROFILE_URL).toString()
        Log.d("tag", url)
        Log.d("tag", name)
        tvName.text = name

        Glide.with(ivImage).load(url).into(ivImage)
    }

    private fun redirect() {
        val intent = Intent(
            this@DriverHomeActivity,
            SplashScreenActivity::class.java
        )
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_nav_main_driver)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}