package com.masai.uber.utlis

import android.app.Activity
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType

class UserUtils {
    companion object {
        fun updateToken(context: Context, token: String) {
            val tokenModel = TokenModel(token)
            FirebaseDatabase.getInstance().getReference(KEY_TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().currentUser?.uid.toString())
                .setValue(tokenModel)
                .addOnFailureListener {

                    AestheticDialog.Builder(
                        context as Activity,
                        DialogStyle.TOASTER,
                        DialogType.ERROR)
                        .setTitle(it.message.toString())
                        .show()
                }
                .addOnSuccessListener {

                }
        }
    }

}