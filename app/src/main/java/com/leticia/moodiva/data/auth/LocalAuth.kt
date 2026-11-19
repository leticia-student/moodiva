package com.leticia.moodiva.data.auth

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hash de senha com PBKDF2-HMAC-SHA256 (RNF14).
 *
 * - Um **sal** aleatório por conta: duas pessoas com a mesma senha têm hashes diferentes.
 * - Muitas **iterações**: testar senhas por tentativa e erro fica lento.
 * - A comparação usa `MessageDigest.isEqual`, que leva o mesmo tempo para qualquer entrada.
 *
 * O resultado é guardado em hexadecimal (o `java.util.Base64` só existe a partir do Android 8).
 */
class PasswordHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {

    data class Hashed(val hash: String, val salt: String)

    fun hash(password: String): Hashed {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return Hashed(hash = derive(password, salt).toHex(), salt = salt.toHex())
    }

    fun verify(password: String, hash: String, salt: String): Boolean {
        val expected = hash.hexToBytesOrNull() ?: return false
        val saltBytes = salt.hexToBytesOrNull() ?: return false
        return MessageDigest.isEqual(derive(password, saltBytes), expected)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toIntOrNull(16)?.toByte() ?: return null
        }
    }

    companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 120_000
        const val SALT_BYTES = 16
        const val KEY_BITS = 256
    }
}

/** Guarda o uid da conta logada no aparelho (planejamento, seção 5). */
interface SessionStore {
    val userId: StateFlow<String?>
    fun save(userId: String)
    fun clear()
}

/**
 * [SessionStore] com `SharedPreferences`. Só o uid é guardado (nada de senha).
 * Com o uid em memória num [StateFlow], a tela reage na hora ao login e ao logout.
 */
class SharedPrefsSessionStore(context: Context) : SessionStore {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(preferences.getString(KEY_USER_ID, null))

    override val userId: StateFlow<String?> = state.asStateFlow()

    override fun save(userId: String) {
        preferences.edit().putString(KEY_USER_ID, userId).apply()
        state.value = userId
    }

    override fun clear() {
        preferences.edit().remove(KEY_USER_ID).apply()
        state.value = null
    }

    private companion object {
        const val PREFS_NAME = "moodiva_session"
        const val KEY_USER_ID = "user_id"
    }
}
