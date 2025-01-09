package com.raju.encryptdatastore

import android.os.Build
import androidx.datastore.core.Serializer
import com.raju.encryptdatastore.util.Crypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64

@Serializable
data class UserPreferences(
    val token: String? = null
)

object UserPreferencesSerializer : Serializer<UserPreferences> {

    override val defaultValue: UserPreferences
        get() = UserPreferences()

    override suspend fun writeTo(
        pref: UserPreferences, output: OutputStream
    ) {
        val json = Json.encodeToString(pref)
        val bytes = json.toByteArray()
        val encryptedBytes = Crypto.encrypt(bytes)
        val encryptedToBytesBase64 = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getEncoder().encodeToString(encryptedBytes)
            } else {
                android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        withContext(Dispatchers.IO) {
            output.use {
                it.write(encryptedToBytesBase64.toByteArray())
            }
        }
    }

    override suspend fun readFrom(input: InputStream): UserPreferences {
        val encryptedByte = withContext(Dispatchers.IO) {
            input.use {
                it.readBytes()
            }
        }

        val encryptedBytesDecoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getDecoder().decode(encryptedByte)
        } else {
            android.util.Base64.decode(encryptedByte, android.util.Base64.DEFAULT)
        }
        val decryptedBytes = Crypto.decrypt(encryptedBytesDecoded)
        val decodedJsonString = decryptedBytes.decodeToString()
        return Json.decodeFromString(decodedJsonString)
    }

}