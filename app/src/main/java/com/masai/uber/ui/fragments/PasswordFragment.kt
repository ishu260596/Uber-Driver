package com.masai.uber.ui.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.masai.uber.databinding.FragmentPasswordBinding
import com.masai.uber.ui.activities.DriverDetailsActivity
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType


class PasswordFragment : Fragment() {
    private var binding: FragmentPasswordBinding? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var userDatabaseRef: DatabaseReference

    private lateinit var userId: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPasswordBinding.inflate(layoutInflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()

        userId = mAuth.currentUser?.uid.toString()
        userDatabaseRef = FirebaseDatabase.getInstance().reference

        binding!!.btnNextPassword.setOnClickListener {
            password = binding!!.etPassword.text.toString()
            if (password.length > 6) {
                redirect()
            } else {
                binding!!.etPassword.error = "password length must be greater than 6"
            }
        }
    }

    private fun redirect() {
        startActivity(
            Intent(context, DriverDetailsActivity::class.java)
                .putExtra("password", password)
        )
        activity?.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}