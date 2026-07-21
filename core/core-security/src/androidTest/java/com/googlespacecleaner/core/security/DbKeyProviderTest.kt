package com.googlespacecleaner.core.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test instrumenté (nécessite un appareil ou émulateur) car
 * EncryptedSharedPreferences dépend de l'Android Keystore, indisponible
 * en test unitaire JVM pur.
 */
@RunWith(AndroidJUnit4::class)
class DbKeyProviderTest {

    @Test
    fun passphraseIsStableAcrossCalls() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = DbKeyProvider(context)

        val first = provider.getOrCreatePassphrase()
        val second = provider.getOrCreatePassphrase()

        assertArrayEquals(first, second)
        assertEquals(32, first.size) // 256 bits
    }

    @Test
    fun rotatePassphraseGeneratesANewKey() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = DbKeyProvider(context)

        val before = provider.getOrCreatePassphrase()
        provider.rotatePassphrase()
        val after = provider.getOrCreatePassphrase()

        org.junit.Assert.assertFalse(before.contentEquals(after))
    }
}
