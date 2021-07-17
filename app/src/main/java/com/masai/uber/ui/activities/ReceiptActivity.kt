package com.masai.uber.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.masai.uber.R
import kotlinx.android.synthetic.main.activity_receipt.*

class ReceiptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)
        btnReceipt.setOnClickListener {
            startActivity(Intent(this, PaymentGateWayActivity::class.java))
        }
    }
}