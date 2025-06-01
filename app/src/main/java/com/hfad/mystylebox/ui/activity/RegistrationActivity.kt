    package com.hfad.mystylebox.ui.activity

    import android.app.Activity
    import android.app.AlertDialog
    import android.content.Intent
    import android.graphics.Bitmap
    import android.graphics.Color
    import android.net.Uri
    import android.os.Bundle
    import android.provider.MediaStore
    import android.text.Editable
    import android.text.TextWatcher
    import android.util.Log
    import android.util.Patterns
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.BaseAdapter
    import android.widget.ImageView
    import android.widget.TextView
    import android.widget.Toast
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.content.FileProvider
    import com.bumptech.glide.Glide
    import com.google.android.gms.auth.api.signin.GoogleSignIn
    import com.google.android.gms.auth.api.signin.GoogleSignInClient
    import com.google.android.gms.auth.api.signin.GoogleSignInOptions
    import com.google.android.gms.common.api.ApiException
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.FirebaseAuthUserCollisionException
    import com.google.firebase.auth.FirebaseUser
    import com.google.firebase.auth.GoogleAuthProvider
    import com.google.firebase.auth.UserProfileChangeRequest
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.storage.FirebaseStorage
    import com.hfad.mystylebox.MainActivity
    import com.hfad.mystylebox.R
    import com.hfad.mystylebox.databinding.ActivityRegistrationBinding
    import com.yalantis.ucrop.UCrop
    import java.io.File
    import java.io.FileOutputStream
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale

    class RegistrationActivity : AppCompatActivity() {

        private lateinit var binding: ActivityRegistrationBinding
        private lateinit var googleSignInClient: GoogleSignInClient
        private lateinit var auth: FirebaseAuth
        private var photoUri: Uri? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityRegistrationBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.tilPassword.helperText = "Минимум 7 символов"
            binding.etPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s != null && s.length >= 7) {
                        binding.tilPassword.error = null
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            binding.etconfirnPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val pass = binding.etPassword.text.toString()
                    val confirm = s?.toString() ?: ""
                    if (confirm == pass) {
                        binding.tilconfirnPassword.error = null
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)

            binding.buttonLoginwithgoogle.setOnClickListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }

            auth = FirebaseAuth.getInstance()

            binding.imageButtonBack.setOnClickListener { onBackPressed() }

            binding.imageAvatar.setOnClickListener {
                showImagePickerDialog()
            }

            binding.buttonRegister.setOnClickListener {
                registerUser()
            }
        }


        // Метод для выбора изображения (галерея, камера, файлы)
        private fun showImagePickerDialog() {
            val options = arrayOf("Выбрать из галереи", "Сфотографировать", "Выбрать из файлов")
            val icons = arrayOf(R.drawable.gallery, R.drawable.ic_camera, R.drawable.ic_file)
            val adapterDialog = object : BaseAdapter() {
                override fun getCount() = options.size
                override fun getItem(position: Int) = options[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val ctx = parent?.context ?: this@RegistrationActivity  // или binding.root.context
                    val view = convertView ?: LayoutInflater.from(ctx)
                        .inflate(R.layout.dialog_item, parent, false)
                    view.findViewById<ImageView>(R.id.icon)
                        .setImageResource(icons[position])
                    view.findViewById<TextView>(R.id.text)
                        .text = options[position]
                    return view
                }
            }
            AlertDialog.Builder(this)
                .setTitle("Выберите действие")
                .setAdapter(adapterDialog) { _, which ->
                    when (which) {
                        0 -> openGallery()
                        1 -> openCamera()
                        2 -> openFiles()
                    }
                }
                .show()
        }

        private fun openGallery() {
            val intent = Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            }
            galleryLauncher.launch(intent)
        }
        private fun openCamera() {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                this, "${this.packageName}.fileprovider", photoFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            cameraLauncher.launch(intent)
        }
        private fun openFiles() {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            fileLauncher.launch(intent)
        }
        private fun createImageFile(): File {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = this.getExternalFilesDir(null)
            return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
        }

        private val galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { startCrop(it) }
            }
        }

        private val cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                photoUri?.let { startCrop(it) }
            }
        }

        private val fileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { startCrop(it) }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
                // Получаем URI обрезанной картинки; может вернуть null
                val outUri = UCrop.getOutput(data ?: return)
                if (outUri != null) {
                    // Сохраняем файл локально и обновляем поле в Firestore
                    val localPath = saveCroppedImageLocally(outUri, "${auth.currentUser!!.uid}.png")
                    photoUri = Uri.fromFile(File(localPath))
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(auth.currentUser!!.uid)
                        .update("localAvatarPath", localPath)

                    // Отображаем в ImageView
                    Glide.with(this)
                        .load(photoUri)
                        .circleCrop()
                        .into(binding.imageAvatar)
                }
                return
            }

            if (requestCode == RC_SIGN_IN) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun startCrop(sourceUri: Uri) {
            val cropFile = File(cacheDir, "crop_${System.currentTimeMillis()}.png")
            cropFile.parentFile?.mkdirs()
            val destinationUri = Uri.fromFile(cropFile)

            val options = UCrop.Options().apply {
                setCircleDimmedLayer(true)
                setToolbarTitle("Кадрирование")
                setToolbarColor(Color.parseColor("#E8A598"))
                setHideBottomControls(false)
                setCompressionFormat(Bitmap.CompressFormat.PNG)
                setCompressionQuality(100)
            }

            UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(500, 500)
                .withOptions(options)
                .start(this)
        }

        private fun firebaseAuthWithGoogle(idToken: String) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser!!
                        saveUserToFirestore(user)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Firebase auth failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        companion object {
            private const val RC_SIGN_IN = 9001
        }

        private fun registerUser() {
            val name     = binding.entername.text.toString().trim()
            val email    = binding.enteremail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm  = binding.etconfirnPassword.text.toString().trim()

            if (name.isEmpty()) {
                binding.entername.error = "Введите имя"
                return
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.enteremail.error = "Неверный e-mail"
                return
            }
            if (password.length < 7) {
                binding.tilPassword.error = "Минимум 7 символов"
                return
            }
            if (password != confirm) {
                binding.tilconfirnPassword.error = "Пароли не совпадают"
                return
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        when (task.exception) {
                            is FirebaseAuthUserCollisionException ->
                                binding.enteremail.error = "Этот e-mail уже зарегистрирован"
                            else ->
                                Toast.makeText(this,
                                    "Ошибка регистрации: ${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG).show()
                        }
                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser!!

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { updTask ->
                            if (!updTask.isSuccessful) {
                                Toast.makeText(this,
                                    "Не удалось установить имя: ${updTask.exception?.message}",
                                    Toast.LENGTH_LONG).show()
                                return@addOnCompleteListener
                            }

                            if (photoUri != null) {
                                // Инициализируем Storage из конфига
                                val storage = FirebaseStorage.getInstance()
                                val avatarRef = storage.reference
                                    .child("avatars/${user.uid}.jpg")

                                avatarRef.putFile(photoUri!!)
                                    .continueWithTask { taskUpload ->
                                        if (!taskUpload.isSuccessful) throw taskUpload.exception!!
                                        avatarRef.downloadUrl
                                    }
                                    .addOnSuccessListener { downloadUri ->
                                        saveProfileToFirebase(user, downloadUri)
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Ошибка загрузки фото: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                saveProfileToFirebase(user, null)
                            }

                            user.sendEmailVerification()
                                .addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        Toast.makeText(this,
                                            "Вы успешно зарегистрированы! Письмо отправлено на $email",
                                            Toast.LENGTH_LONG).show()
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this,
                                            "Ошибка отправки письма: ${verifyTask.exception?.message}",
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                }
        }

        private fun saveUserToFirestore(user: FirebaseUser) {
            val db = FirebaseFirestore.getInstance()
            // Формируем данные, которые хотим сохранить
            val userData = mapOf(
                "displayName" to (user.displayName ?: ""),
                "email"       to (user.email ?: ""),
                "photoUrl"    to (user.photoUrl?.toString() ?: "")
            )

            db.collection("users")
                .document(user.uid)    // ключ — uid пользователя
                .set(userData)
                .addOnSuccessListener { Log.d("Firestore", "User saved") }
                .addOnFailureListener { e -> Log.e("Firestore", "Error saving user", e) }
        }

        private fun saveCroppedImageLocally(sourceUri: Uri, filename: String): String {
            val input = contentResolver.openInputStream(sourceUri)!!
            // создаём папку avatars
            val avatarDir = File(filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val outFile = File(avatarDir, filename)
            FileOutputStream(outFile).use { fos ->
                input.copyTo(fos)
            }
            input.close()
            return outFile.absolutePath
        }

        private fun saveProfileToFirebase(user: FirebaseUser, photoUrl: Uri?) {
            // **3) Обновляем профиль FirebaseAuth окончательно** (т.к. у него будет и displayName, и photoUrl)
            val builder = UserProfileChangeRequest.Builder()
            if (photoUrl != null) builder.setPhotoUri(photoUrl)
            val finalUpdates = builder.build()

            user.updateProfile(finalUpdates)
                .addOnCompleteListener { finalTask ->
                    if (!finalTask.isSuccessful) {
                        Toast.makeText(this, "Не удалось обновить профиль: ${finalTask.exception?.message}", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    // **4) Пишем в Firestore** ровно те же поля
                    val userData = mapOf(
                        "displayName" to (user.displayName ?: ""),
                        "email"       to (user.email ?: ""),
                        "photoUrl"    to (user.photoUrl.toString())
                    )
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            // **5) Отправляем письмо**
                            user.sendEmailVerification()
                                .addOnCompleteListener { vTask ->
                                    if (vTask.isSuccessful) {
                                        Toast.makeText(this, "Письмо отправлено на ${user.email}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this, "Ошибка отправки письма: ${vTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                    // **6) И переходим в MainActivity**
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Не удалось сохранить пользователя: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
        }

    }