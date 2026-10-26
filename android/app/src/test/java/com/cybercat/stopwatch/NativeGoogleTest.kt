package com.cybercat.stopwatch
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
class NativeGoogleTest {
    @Test fun nonceIsFreshAndUrlSafe() {val a=NativeGoogle.nonce();val b=NativeGoogle.nonce();assertNotEquals(a,b);assertTrue(a.matches(Regex("[A-Za-z0-9_-]{43}")))}
    @Test fun supabaseGetsRawNonceAndGoogleGetsItsHash() {val raw=NativeGoogle.nonce();val expected=MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString(""){"%02x".format(it.toInt() and 255)};assertEquals(expected,NativeGoogle.hashNonce(raw));assertEquals(64,expected.length);assertNotEquals(raw,expected)}
}
