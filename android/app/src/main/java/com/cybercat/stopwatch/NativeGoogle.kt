package com.cybercat.stopwatch

import android.app.Activity
import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Native account picker only. ID tokens are verified by Supabase, never trusted locally. */
object NativeGoogle {
    const val WEB_CLIENT_ID="265421497125-o0i3rkkj1mlckg0bnup48o67i3s0fm4n.apps.googleusercontent.com"
    class Identity(val token:String,val rawNonce:String)
    fun nonce():String=Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also {SecureRandom().nextBytes(it)})
    fun hashNonce(raw:String):String=MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it.toInt() and 255)}
    suspend fun clearSession(activity:Activity) { CredentialManager.create(activity).clearCredentialState(androidx.credentials.ClearCredentialStateRequest()) }
    suspend fun signIn(activity:Activity):Identity {
        val raw=nonce()
        val option=GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).setNonce(hashNonce(raw)).build()
        val request=GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result=CredentialManager.create(activity).getCredential(MutableContextWrapper(activity),request)
        val credential=result.credential
        check(credential is CustomCredential && credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {"Unexpected sign-in response"}
        return Identity(GoogleIdTokenCredential.createFrom(credential.data).idToken,raw)
    }
}
