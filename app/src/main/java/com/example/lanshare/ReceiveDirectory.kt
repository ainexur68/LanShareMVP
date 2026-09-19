package com.example.lanshare

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/** Intents for opening the folder where verified incoming files are published. */
object ReceiveDirectory {
    private const val DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"
    const val RELATIVE_PATH = "Download/LanShare"
    const val DOCUMENT_ID = "primary:Download/LanShare"
    const val VIEW_URI = "content://$DOCUMENTS_AUTHORITY/document/primary%3ADownload%2FLanShare"
    const val TREE_URI = "content://$DOCUMENTS_AUTHORITY/tree/primary%3ADownload%2FLanShare"

    data class IntentSpec(
        val action: String,
        val uri: String,
        val mimeType: String? = null
    )

    fun viewSpec() = IntentSpec(
        action = Intent.ACTION_VIEW,
        uri = VIEW_URI,
        mimeType = "vnd.android.document/directory"
    )

    fun treeSpec() = IntentSpec(
        action = Intent.ACTION_OPEN_DOCUMENT_TREE,
        uri = TREE_URI
    )

    fun viewIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        val spec = viewSpec()
        setDataAndType(Uri.parse(spec.uri), spec.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun treeIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        val spec = treeSpec()
        putExtra(
            DocumentsContract.EXTRA_INITIAL_URI,
            Uri.parse(spec.uri)
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}
