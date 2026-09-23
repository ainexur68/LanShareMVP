package com.example.lanshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.lanshare.ui.LanShareApp

class MainActivity : ComponentActivity() {
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        acceptUris(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ContextCompat.startForegroundService(this, Intent(this, TransferService::class.java))
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        consumeShareIntent(intent)
        setContent {
            LanShareApp(
                onPickFiles = { pickFiles.launch(arrayOf("*/*")) },
                onOpenReceiveDirectory = ::openReceiveDirectory
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeShareIntent(intent)
    }

    private fun openReceiveDirectory() {
        val intents = listOf(ReceiveDirectory.viewIntent(), ReceiveDirectory.treeIntent())
        for (candidate in intents) {
            if (candidate.resolveActivity(packageManager) != null &&
                runCatching { startActivity(candidate) }.isSuccess
            ) return
        }
        Toast.makeText(this, "未找到可用的文件管理器", Toast.LENGTH_SHORT).show()
    }

    private fun consumeShareIntent(intent: Intent?) {
        val isExternalShare = intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE
        val uris: List<Uri> = when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }
        if (isExternalShare) {
            // A new share intent represents a new task. Replace the old selection even when
            // the sender supplied no URI, otherwise stale files could be sent accidentally.
            acceptUris(uris, replaceExisting = true)
        }
    }

    private fun acceptUris(uris: List<Uri>, replaceExisting: Boolean = false) {
        if (uris.isEmpty()) {
            if (replaceExisting) AppState.sharedFiles.clear()
            return
        }
        val incoming = uris.mapNotNull { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { FileUtil.meta(contentResolver, uri) }.getOrNull()
        }
        val merged = SharedFileSelection.merge(
            existing = AppState.sharedFiles.toList(),
            incoming = incoming,
            replaceExisting = replaceExisting,
            key = { it.uri.toString() }
        )
        AppState.sharedFiles.clear()
        AppState.sharedFiles.addAll(merged)
    }
}
