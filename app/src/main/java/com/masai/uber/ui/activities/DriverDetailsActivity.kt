package com.masai.uber.ui.activities

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.ybq.android.spinkit.style.Circle
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.masai.uber.R
import com.masai.uber.databinding.ActivityDriverDetailsBinding
import com.masai.uber.databinding.DialogCustomImageSelectionBinding
import com.masai.uber.utlis.*
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType


class DriverDetailsActivity : AppCompatActivity() {
    private var mBinding: ActivityDriverDetailsBinding? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var profilePicRef: StorageReference

    private lateinit var userId: String
    private lateinit var profileImage: String

    private lateinit var fName: String
    private lateinit var lName: String
    private lateinit var email: String
    private lateinit var mobile: String
    private lateinit var taxiNumber: String
    private lateinit var modelNumber: String
    private lateinit var license: String

    companion object {
        private const val CAMERA = 1
        private const val GALLERY = 2
    }

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityDriverDetailsBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)
        initViews()
        mBinding!!.ivAddProfileImage.setOnClickListener {
            customImageSelectionDialog()
        }
        mBinding!!.btnNext.setOnClickListener {
            verifyCredentials()
        }
    }

    private fun initViews() {
        PreferenceHelper.getSharedPreferences(this)
        mAuth = FirebaseAuth.getInstance()
        userId = mAuth.currentUser?.uid.toString()
        databaseRef = FirebaseDatabase.getInstance().reference
        profilePicRef = FirebaseStorage.getInstance().reference
        progressBar = findViewById(R.id.spin_kit)
    }

    private fun verifyCredentials() {
        fName = mBinding!!.etFirstName.text.toString()
        lName = mBinding!!.etLastName.text.toString()
        email = mBinding!!.etEmail.text.toString()
        mobile = mBinding!!.etMobileNumber.text.toString()
        license = mBinding!!.etLicenseNumber.text.toString()
        taxiNumber = mBinding!!.etCarNumber.text.toString()
        modelNumber = mBinding!!.etMobileNumber.text.toString()

        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        if (fName.isEmpty()) {
            mBinding!!.etFirstName.error = "Fill this please !"
            return
        }
        updatePreference()
        saveDataToDatabase()
    }

    private fun saveDataToDatabase() {
        val hashMap: HashMap<String, String> = HashMap<String, String>()
        hashMap["name"] = "$fName $lName"
        hashMap["mobile"] = mobile
        hashMap["email"] = email
        hashMap["license"] = license
        hashMap["taxinumber"] = taxiNumber
        hashMap["model"] = modelNumber
        hashMap["profileurl"] = profileImage
        databaseRef.child("Drivers").child(userId).setValue(hashMap)
            .addOnCompleteListener {
                AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.SUCCESS)
                    .setTitle("Success")
                    .show()
                redirect()
            }
            .addOnFailureListener {
                AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.ERROR)
                    .setTitle("Failed")
                    .show()
            }
    }

    private fun updatePreference() {
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_DISPLAY_NAME, "$fName $lName")
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_MOBILE_NUMBER, mobile)
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_TAXI_MODEL, modelNumber)
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_MOBILE_NUMBER, mobile)
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_TAXI_MODEL, modelNumber)
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_GOOGLE_GMAIL, email)
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_TAXI_NUMBER, taxiNumber)
        PreferenceHelper.writeStringToPreference(KEY_DRIVER_PROFILE_URL, profileImage)
    }

    private fun redirect() {
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }


    /**
     * A function to launch the custom image selection dialog.
     */
    private fun customImageSelectionDialog() {
        val dialog = Dialog(this@DriverDetailsActivity)

        val binding: DialogCustomImageSelectionBinding =
            DialogCustomImageSelectionBinding.inflate(layoutInflater)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        dialog.setContentView(binding.root)
        binding.tvCamera.setOnClickListener {

            Dexter.withContext(this@DriverDetailsActivity)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            // Here after all the permission are granted launch the CAMERA to capture an image.
                            if (report.areAllPermissionsGranted()) {
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                startActivityForResult(intent, CAMERA)
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
            dialog.dismiss()
        }

        binding.tvGallery.setOnClickListener {
            Dexter.withContext(this@DriverDetailsActivity)
                .withPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .withListener(object : PermissionListener {

                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                        // Here after all the permission are granted launch the gallery to select and image.
                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        startActivityForResult(galleryIntent, GALLERY)
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        AestheticDialog.Builder(
                            this@DriverDetailsActivity,
                            DialogStyle.TOASTER,
                            DialogType.WARNING
                        )
                            .setTitle("Warning !")
                            .setMessage("You have denied the storage permission to select image.")
                            .show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()

            dialog.dismiss()
        }
        //Start the dialog and display it on screen.
        dialog.show()
    }

    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val cubeGrid: Circle = Circle()
        progressBar.indeterminateDrawable = cubeGrid
        val filePath: StorageReference = profilePicRef.child("DriverProfile")
            .child("$userId.jpg")
        if (requestCode == CAMERA) {
            progressBar.visibility = View.VISIBLE
            data?.extras?.let {
                val thumbnail: Uri =
                    data.extras!!.get("data") as Uri // Bitmap from camera
                mBinding!!.profileImage.setImageURI(thumbnail)
                if (thumbnail != null) {
                    val uploadTask: UploadTask = filePath.putFile(thumbnail)
                    uploadTask.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot?> {
                        profilePicRef.child("DriverProfile")
                            .child("$userId.jpg").downloadUrl.addOnSuccessListener(
                                OnSuccessListener<Uri> { uri ->
                                    val url = uri.toString()
                                    profileImage = url.toString()
                                    progressBar.visibility = View.GONE
                                    AestheticDialog.Builder(
                                        this@DriverDetailsActivity,
                                        DialogStyle.TOASTER,
                                        DialogType.SUCCESS
                                    )
                                        .setTitle("Uploaded Successfully")
                                        .show()
                                })
                    })

                }
                // Set Capture Image bitmap to the imageView using Glide
                /**  Glide.with(this@DriverDetailsActivity)
                .load(thumbnail)
                .centerCrop()
                .into(mBinding!!.profileImage) **/

                // Replace the add icon with edit icon once the image is loaded.
                mBinding!!.ivAddProfileImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@DriverDetailsActivity,
                        R.drawable.ic_vector_edit
                    )
                )
            }
        } else if (requestCode == GALLERY) {
            progressBar.visibility = View.VISIBLE
            data?.let {
                // Here we will get the select image URI.
                val thumbnail: Uri? = data.data
                mBinding!!.profileImage.setImageURI(thumbnail)
                if (thumbnail != null) {
                    val uploadTask: UploadTask = filePath.putFile(thumbnail)
                    uploadTask.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot?> {
                        profilePicRef.child("DriverProfile")
                            .child("$userId.jpg").downloadUrl.addOnSuccessListener(
                                OnSuccessListener<Uri> { uri ->
                                    val url = uri.toString()
                                    profileImage = url.toString()
                                    progressBar.visibility = View.GONE
                                    AestheticDialog.Builder(
                                        this@DriverDetailsActivity,
                                        DialogStyle.TOASTER,
                                        DialogType.SUCCESS
                                    )
                                        .setTitle("Uploaded Successfully")
                                        .show()
                                })
                    })

                }

                // Set Selected Image URI to the imageView using Glide
                /**  Glide.with(this@DriverDetailsActivity)
                .load(selectedPhotoUri)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                @Nullable e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
                ): Boolean {
                // log exception
                Log.e("TAG", "Error loading image", e)
                return false // important to return false so the error placeholder can be placed
                }

                override fun onResourceReady(
                resource: Drawable,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
                ): Boolean {
                val bitmap: Bitmap = resource.toBitmap()
                return false
                }
                })
                .into(mBinding!!.profileImage)  **/

                // Replace the add icon with edit icon once the image is selected.
                mBinding!!.ivAddProfileImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@DriverDetailsActivity,
                        R.drawable.ic_vector_edit
                    )
                )
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("tag", "Cancelled")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding = null
    }
}