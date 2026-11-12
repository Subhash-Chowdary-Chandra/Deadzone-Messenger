package com.deadzone.messenger.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Utility object for storing and retrieving avatar images (as Base64 JPEG strings)
 * using SharedPreferences. Own avatar and peer avatars are kept separate.
 */
object AvatarStore {

    private const val PREFS_NAME = "DeadzonePrefs"
    private const val MY_AVATAR_KEY = "MY_AVATAR_BASE64"
    private const val AVATAR_PREFIX = "AVATAR_"
    private const val TARGET_SIZE = 100  // px – thumbnail dimension
    private const val JPEG_QUALITY = 75  // 0-100

    // ── Own avatar ────────────────────────────────────────────────────────────

    fun saveMyAvatar(context: Context, base64: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(MY_AVATAR_KEY, base64)
            .apply()
    }

    fun getMyAvatar(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(MY_AVATAR_KEY, null)

    fun clearMyAvatar(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(MY_AVATAR_KEY)
            .apply()
    }

    // ── Peer avatars ──────────────────────────────────────────────────────────

    fun savePeerAvatar(context: Context, deviceId: String, base64: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AVATAR_PREFIX + deviceId, base64)
            .apply()
    }

    fun getPeerAvatar(context: Context, deviceId: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(AVATAR_PREFIX + deviceId, null)

    // ── Conversion helpers ────────────────────────────────────────────────────

    /**
     * Decode a Base64 string to a Bitmap. Returns null on any error.
     */
    fun base64ToBitmap(base64: String): Bitmap? = try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }

    /**
     * Resize [bitmap] to [TARGET_SIZE]×[TARGET_SIZE], then JPEG-encode and
     * Base64-encode the result. Safe to call on any thread.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, TARGET_SIZE, TARGET_SIZE, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        if (scaled != bitmap) scaled.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }
}
