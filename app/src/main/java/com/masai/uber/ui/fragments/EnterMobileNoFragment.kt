package com.masai.uber.ui.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.masai.uber.databinding.FragmentEnterMobileNoBinding
import com.masai.uber.ui.activities.PhoneVerificationActivity
import com.masai.uber.ui.activities.SocialActivity



class FragmentEnterMobileNo : Fragment() {
    private var binding: FragmentEnterMobileNoBinding? = null
    private lateinit var mobileNumber: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEnterMobileNoBinding.inflate(layoutInflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.tvSocialMedia.setOnClickListener {
            startActivity(
                Intent(requireContext(), SocialActivity::class.java))
            activity?.finish()
        }

        binding!!.btnNext.setOnClickListener {
            mobileNumber = binding!!.etMobileNumber.text.toString()
            if (mobileNumber.length == 10) {
                val intent = Intent(requireContext(), PhoneVerificationActivity::class.java)
                intent.putExtra("number", binding!!.etMobileNumber.text.toString())
                startActivity(intent)
                activity?.finish()
            } else binding!!.etMobileNumber.error = "Please provide a valid mobile number"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}