package com.example.myapplication

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Wires up "take a photo / pick from gallery" + multipart upload to
 * /api/photos for a fragment. Must be constructed as a field (not inside a
 * click listener) so the ActivityResultContracts launchers register before
 * the fragment reaches STARTED, per androidx requirements.
 *
 * Set [inspectionActId] xor [replacementActId] before calling [showChooser];
 * exactly one must be set, matching the backend's upload contract.
 */
class PhotoCaptureHelper(
    private val fragment: Fragment,
    private val onUploaded: () -> Unit
) {
    var inspectionActId: Long? = null
    var replacementActId: Long? = null

    private var pendingCameraUri: Uri? = null

    private val takePicture = fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { upload(it) }
    }
    private val pickImage = fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { upload(it) }
    }
    private val requestCameraPermission =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
        }

    fun showChooser() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Добавить фото")
            .setItems(arrayOf("Камера", "Галерея")) { _, which ->
                if (which == 0) requestCameraThenLaunch() else pickImage.launch("image/*")
            }
            .show()
    }

    private fun requestCameraThenLaunch() {
        val granted = ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val file = File.createTempFile("photo_", ".jpg", fragment.requireContext().cacheDir)
        val uri = FileProvider.getUriForFile(
            fragment.requireContext(), "${fragment.requireContext().packageName}.fileprovider", file
        )
        pendingCameraUri = uri
        takePicture.launch(uri)
    }

    private fun upload(uri: Uri) {
        val context = fragment.requireContext()
        fragment.lifecycleScope.launch {
            val (bytes, mime) = withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "image/jpeg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                bytes to mime
            }
            if (bytes.isEmpty()) {
                Toast.makeText(context, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val filePart = MultipartBody.Part.createFormData(
                "file", "photo_${System.currentTimeMillis()}.jpg", bytes.toRequestBody(mime.toMediaTypeOrNull())
            )
            val inspPart = inspectionActId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val replPart = replacementActId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            runCatching { ApiClient.api.uploadPhoto(filePart, inspPart, replPart, null) }
                .onSuccess {
                    Toast.makeText(context, "Фото добавлено", Toast.LENGTH_SHORT).show()
                    onUploaded()
                }
                .onFailure {
                    Toast.makeText(context, it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
        }
    }
}
