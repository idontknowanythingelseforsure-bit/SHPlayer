package com.shplayer.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import java.io.InputStream

/**
 * Album artwork loader.
 *
 * MediaProvider has changed album-art access repeatedly across Android versions,
 * so this tries the legacy `albumart` endpoint first, then a song thumbnail
 * (Android 10+), then the deprecated `ALBUM_ART` file path.
 *
 * OPTIMIZATION HOOK: memory-only [LruCache]. For large libraries add a disk
 * cache keyed by `(albumId, size)`; [load] is the only entry point the UI uses.
 */
object ArtworkLoader {

    private const val LEGACY_ALBUM_ART_URI = "content://media/external/audio/albumart"

    private val cache = object : LruCache<Long, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    fun load(context: Context, albumId: Long, sizePx: Int): Bitmap? {
        if (albumId <= 0L) return null
        cache.get(albumId)?.let { return it }

        val target = sizePx.coerceAtLeast(96)
        val bitmap = runCatching { readArtwork(context, albumId, target) }.getOrNull() ?: return null
        cache.put(albumId, bitmap)
        return bitmap
    }

    private fun readArtwork(context: Context, albumId: Long, sizePx: Int): Bitmap? {
        // 1. Legacy album-art endpoint. Still served by MediaProvider on most
        //    devices, including Android 10+.
        readAlbumArtEndpoint(context, albumId, sizePx)?.let { return it }

        // 2. Thumbnail of a representative song in the album (Android 10+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val songId = firstSongId(context, albumId)
            if (songId > 0L) {
                val songUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songId,
                )
                runCatching {
                    context.contentResolver.loadThumbnail(songUri, Size(sizePx, sizePx), null)
                }.getOrNull()?.let { return it }
            }
        }

        // 3. Deprecated file path column (Android 9 and below, some vendors).
        return readAlbumArtFile(context, albumId, sizePx)
    }

    private fun readAlbumArtEndpoint(context: Context, albumId: Long, sizePx: Int): Bitmap? {
        val uri = Uri.parse("$LEGACY_ALBUM_ART_URI/$albumId")
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                decodeStream(stream, sizePx)
            }
        }.getOrNull()
    }

    private fun readAlbumArtFile(context: Context, albumId: Long, sizePx: Int): Bitmap? {
        @Suppress("DEPRECATION")
        val projection = arrayOf(MediaStore.Audio.Albums.ALBUM_ART)
        val selection = "${MediaStore.Audio.Albums._ID} = ?"
        val args = arrayOf(albumId.toString())

        val path = runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        }.getOrNull() ?: return null

        return runCatching { decodeFile(path, sizePx) }.getOrNull()
    }

    private fun firstSongId(context: Context, albumId: Long): Long = runCatching {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID),
            "${MediaStore.Audio.Media.ALBUM_ID} = ?",
            arrayOf(albumId.toString()),
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L } ?: 0L
    }.getOrDefault(0L)

    private fun decodeStream(stream: InputStream, sizePx: Int): Bitmap? {
        val bytes = stream.readBytes()
        return decodeBytes(bytes, sizePx)
    }

    private fun decodeFile(path: String, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
        }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun decodeBytes(bytes: ByteArray, sizePx: Int): Bitmap? {
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int, sizePx: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= sizePx && height / (sample * 2) >= sizePx) {
            sample *= 2
        }
        return sample
    }

    /** Artwork URI handed to Media3 for the media notification. */
    fun albumArtUri(albumId: Long): Uri = Uri.parse("$LEGACY_ALBUM_ART_URI/$albumId")
}
