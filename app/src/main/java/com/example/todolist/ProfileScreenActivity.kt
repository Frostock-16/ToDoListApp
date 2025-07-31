package com.example.todolist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.example.todolist.showDialogFragUtil.showDialogFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class ProfileScreenActivity : BaseActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var photoUri: Uri
    private lateinit var ivProfileimg: ImageView
    private lateinit var tvAccountName: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profilescreen_activity)
        setupToolbar("Profile", true)

        // Bottom Nav
        bottomNav = findViewById(R.id.bottom_nav)
        BottomNavUtil.setUpBottomNav(this@ProfileScreenActivity, bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile

        // Hide/Show bottom nav
        val scrollView = findViewById<ScrollView>(R.id.scrollViewProf)
        var lastScrollY = 0
        val scrollThreshold = 10

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val currentScrollY = scrollView.scrollY
            val dy = currentScrollY - lastScrollY
            if (dy > scrollThreshold) {
                // Scrolling down
                bottomNav.animate().translationY(bottomNav.height.toFloat()).setDuration(300).start()
            } else if (dy < -scrollThreshold) {
                // Scrolling up
                bottomNav.animate().translationY(0f).setDuration(300).start()
            }

            lastScrollY = currentScrollY
        }

        // Set profile image
        ivProfileimg = findViewById(R.id.iv_profileimg)
        loadProfileImageFromStorage()
        ivProfileimg.setOnClickListener {
            showImagePickerDialog()
        }

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        val bitmap = uriToBitmap(uri)
                        saveProfileImage(bitmap)
                        ivProfileimg.setImageBitmap(bitmap)
                    }
                }
            }

        takePhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val bitmap = uriToBitmap(photoUri)
                    saveProfileImage(bitmap)
                    ivProfileimg.setImageBitmap(bitmap)
                }
            }

        // TextView
        tvAccountName = findViewById(R.id.tv_accountname)
        val tvTaskLeft = findViewById<TextView>(R.id.tv_taskleft)
        val tvTaskDone = findViewById<TextView>(R.id.tv_taskdone)

        val taskLeftCount = getSharedPreferences("ToDoList", MODE_PRIVATE).getString("TaskLeftCount", "0")
        val taskDoneCount = getSharedPreferences("ToDoList", MODE_PRIVATE).getInt("TaskDoneCount", 0)
        tvTaskLeft.text = "${taskLeftCount} Task Left"
        tvTaskDone.text = "${taskDoneCount.toString()} Task Done"


        // Fragment
        val addTaskDialogFragment = FirebaseAuth.getInstance().currentUser?.displayName?.let {
            AddTaskDialogFragment(it)
        }

        // Linear layout
        val llChangeAccName = findViewById<LinearLayout>(R.id.ll_changeaccoutname)
        val llChangeEmail = findViewById<LinearLayout>(R.id.ll_changeemail)
        val llChangePassword = findViewById<LinearLayout>(R.id.ll_changepassword)
        val llAppSettings = findViewById<LinearLayout>(R.id.ll_app_settings)
        llAppSettings.setOnClickListener {
            val intent = Intent(this, AppSettingsActivity::class.java)
            startActivity(intent)
        }
        val llLogOut = findViewById<LinearLayout>(R.id.ll_logout)


        llChangeAccName.setOnClickListener {
            if (addTaskDialogFragment != null) {
                showDialogFragment(
                    addTaskDialogFragment,
                    supportFragmentManager,
                    "hideViews_ProfileScreen",
                    "AddTaskDialogFragment"
                )
                addTaskDialogFragment.setOnAddTaskListener(object :
                    AddTaskDialogFragment.onAddTaskListener {
                    override fun onAddTask(title: String, description: String) {
                        val user = FirebaseAuth.getInstance().currentUser
                        tvAccountName.text = title
                        user?.updateProfile(userProfileChangeRequest { displayName = title })
                    }
                })
            }
        }

        llChangeEmail.setOnClickListener {
            val dialog = FirebaseAuth.getInstance().currentUser?.email?.let { it1 ->
                AddTaskDialogFragment(it1)
            }
            val user = FirebaseAuth.getInstance().currentUser
            if (user?.providerData?.any { it.providerId == "google.com" } == true) {
                AlertDialog.Builder(this)
                    .setTitle("Google Sign-In")
                    .setMessage("Your account is linked with Google. To change your email, please update it in your Google Account settings.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                if (dialog != null) {
                    dialog.setOnAddTaskListener(object : AddTaskDialogFragment.onAddTaskListener {
                        override fun onAddTask(title: String, description: String) {
                            showReAuthDialog(title)
                        }
                    })
                    showDialogFragment(
                        dialog,
                        supportFragmentManager,
                        "hideViews_ProfileScreenEmail",
                        "AddTaskDialogFragment"
                    )
                }
            }
        }

        llChangePassword.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user?.providerData?.any { it.providerId == "google.com" } == true) {
                AlertDialog.Builder(this)
                    .setTitle("Google Sign-In")
                    .setMessage("Your account is linked with Google. To change your password, please update it in your Google Account settings.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                val dialog = AddTaskDialogFragment()
                dialog.setOnAddTaskListener(object : AddTaskDialogFragment.onAddTaskListener {
                    override fun onAddTask(newPassword: String, ignored: String) {
                        showReAuthDialogForPassword(newPassword)
                    }
                })
                showDialogFragment(
                    dialog,
                    supportFragmentManager,
                    "hideViews_ProfileScreenPassword",
                    "AddTaskDialogFragment"
                )
            }
        }


        llLogOut.setOnClickListener {
            val googleSignInClient = GoogleSignIn.getClient(
                this,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            )
            googleSignInClient.signOut().addOnCompleteListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }


        setBackToHome()
        loadAccountNameFromFirebase()
    }

    // Profile Image (local)
    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Set Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun openCamera() {
        val imageFile = File.createTempFile("profile_", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        takePhotoLauncher.launch(intent)
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        val prefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
        prefs.getString("ProfileImagePath", null)?.let { oldFileName ->
            val oldFile = File(filesDir, oldFileName)
            if (oldFile.exists()) oldFile.delete()
        }

        val fileName = "profile_image_${System.currentTimeMillis()}.jpg"
        openFileOutput(fileName, MODE_PRIVATE).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        prefs.edit().putString("ProfileImagePath", fileName).apply()
    }


    private fun loadProfileImageFromStorage() {
        val prefs = getSharedPreferences("ToDoList", MODE_PRIVATE)
        val fileName = prefs.getString("ProfileImagePath", null)
        if (fileName != null) {
            val file = File(filesDir, fileName)
            if (file.exists()) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                ivProfileimg.setImageBitmap(bitmap)
            }
        }
    }


    // Change Account Name
    private fun loadAccountNameFromFirebase() {
        val user = FirebaseAuth.getInstance().currentUser
        val nameToDisplay = user?.displayName ?: user?.email ?: "No Name"
        tvAccountName.text = nameToDisplay
    }


    // Change Email
    private fun showReAuthDialog(newEmail: String) {
        val input = EditText(this).apply {
            hint = "Enter current password"
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Re-authenticate")
            .setMessage("Please enter your current password to confirm.")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val currentPassword = input.text.toString()
                updateEmailWithReAuth(currentPassword, newEmail)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmailWithReAuth(currentPassword: String, newEmail: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val currentEmail = user?.email

        if (user != null && currentEmail != null) {
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            user.reauthenticate(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        user.verifyBeforeUpdateEmail(newEmail)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    user.sendEmailVerification()
                                    Toast.makeText(
                                        this,
                                        "Email updated. Verification sent.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Update failed: ${updateTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Re-auth failed: wrong password?", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    // Change Password
    private fun showReAuthDialogForPassword(newPassword: String) {
        val input = EditText(this).apply {
            hint = "Enter current password"
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Re-authenticate")
            .setMessage("Please enter your current password to confirm.")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val currentPassword = input.text.toString()
                updatePasswordWithReAuth(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePasswordWithReAuth(currentPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val currentEmail = user?.email

        if (user != null && currentEmail != null) {
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            user.reauthenticate(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(this, "Password updated.", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Update failed: ${updateTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Re-auth failed: wrong password?", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    // Profile Image (Storage Firebase)
//    private fun uploadProfileImage(originalUri: Uri) {
//        Log.d("ProfileImageUpload", "Preparing to upload. Original URI: $originalUri")
//
//        if (userId == null) {
//            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Ensure we have a local copy
//        val localFile = File(cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
//        contentResolver.openInputStream(originalUri)?.use { input ->
//            localFile.outputStream().use { output ->
//                input.copyTo(output)
//            }
//        }
//        val safeUri = Uri.fromFile(localFile)
//        Log.d("ProfileImageUpload", "Local copy URI: $safeUri")
//
//        val storageRef = storage.reference.child("profile_pics/$userId.jpg")
//        storageRef.putFile(safeUri)
//            .addOnSuccessListener {
//                Log.d("ProfileImageUpload", "Upload success. Fetching download URL.")
//                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
//                    Log.d("ProfileImageUpload", "Download URL: $downloadUrl")
//                    db.collection("users").document(userId)
//                        .update("profileImageUrl", downloadUrl.toString())
//                    Toast.makeText(this, "Profile image updated.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("ProfileImageUpload", "Upload failed", e)
//                Toast.makeText(this, "Failed to upload profile image.", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//
//
//    private fun loadProfileImage() {
//        if (userId != null) {
//            db.collection("users").document(userId).get()
//                .addOnSuccessListener { document ->
//                    val imageUrl = document.getString("profileImageUrl")
//                    if (!imageUrl.isNullOrEmpty()) {
//                        Glide.with(this)
//                            .load(imageUrl)
//                            .circleCrop()
//                            .into(ivProfileimg)
//                    }
//                }
//        }
//    }
}