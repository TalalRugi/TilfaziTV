package app.tilfazi.tv.feed

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.InputStream

/**
 * The photos that are not Unsplash's: the ones built into the app (assets/scenes), and the owner's own on the TV —
 * any image in a folder named «Tilfazi» (on the TV or a USB drive, found through the media library), and any image
 * copied into the app's own folder (Android/data/app.tilfazi.tv/files/photos).
 */
object Gallery {
    const val FOLDER = "Tilfazi"

    /** a photo to show: where to open it from */
    sealed class Pick {
        data class Scene(val name: String) : Pick()
        data class Media(val uri: Uri) : Pick()
        data class Own(val file: File) : Pick()
    }

    fun scenes(c: Context): List<Pick> =
        (c.assets.list("scenes") ?: emptyArray()).filter { it.endsWith(".jpg") }.sorted().map { Pick.Scene(it) }

    fun readPermission() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    fun mayRead(c: Context) = c.checkSelfPermission(readPermission()) == PackageManager.PERMISSION_GRANTED

    fun ownFolder(c: Context): File? = c.getExternalFilesDir("photos")?.apply { mkdirs() }

    /** the owner's photos on the TV, newest first */
    fun owned(c: Context): List<Pick> {
        val found = mutableListOf<Pick>()
        ownFolder(c)?.listFiles()?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            ?.sortedByDescending { it.lastModified() }?.forEach { found += Pick.Own(it) }
        if (mayRead(c)) {
            val volume = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            runCatching {
                c.contentResolver.query(
                    volume, arrayOf(MediaStore.Images.Media._ID),
                    "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? COLLATE NOCASE", arrayOf(FOLDER),
                    "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
                )?.use { cur ->
                    while (cur.moveToNext()) found += Pick.Media(ContentUris.withAppendedId(volume, cur.getLong(0)))
                }
            }
        }
        return found
    }

    /** the photo at about the screen's width, so a 6000 px original is not held in memory */
    fun decode(c: Context, pick: Pick, width: Int = 1920): Bitmap? = runCatching {
        fun open(): InputStream? = when (pick) {
            is Pick.Scene -> c.assets.open("scenes/${pick.name}")
            is Pick.Media -> c.contentResolver.openInputStream(pick.uri)
            is Pick.Own -> pick.file.inputStream()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= width) sample *= 2
        open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
    }.getOrNull()
}
