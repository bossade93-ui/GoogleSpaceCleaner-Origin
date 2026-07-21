package com.googlespacecleaner.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import android.util.Base64

/**
 * Génère et conserve la clé de chiffrement (passphrase) de la base Room
 * chiffrée avec SQLCipher.
 *
 * La clé elle-même est stockée dans un fichier EncryptedSharedPreferences
 * distinct de celui des tokens OAuth (séparation des secrets), protégé par
 * l'Android Keystore (MasterKey). Elle n'est jamais journalisée.
 *
 * Génération : 256 bits de hasard cryptographique à la première ouverture de
 * l'app, puis réutilisée à chaque lancement — perdre cette clé signifie
 * perdre l'accès aux données locales en cache (pas de perte de données Google,
 * seulement du cache local qui peut être régénéré par un nouveau scan).
 */
class DbKeyProvider(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "gs_cleaner_db_key_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Retourne la passphrase existante, ou en génère et persiste une nouvelle. */
    fun getOrCreatePassphrase(): ByteArray {
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }

        val newKey = ByteArray(32) // 256 bits, recommandé pour SQLCipher
        SecureRandom().nextBytes(newKey)

        prefs.edit()
            .putString(KEY_DB_PASSPHRASE, Base64.encodeToString(newKey, Base64.NO_WRAP))
            .apply()

        return newKey
    }

    /**
     * À utiliser uniquement en cas de réinitialisation volontaire (ex: bouton
     * "Effacer toutes les données locales" dans les réglages) — la base Room
     * existante devient illisible et doit être recréée.
     */
    fun rotatePassphrase() {
        prefs.edit().remove(KEY_DB_PASSPHRASE).apply()
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}
