package com.strangeparticle.springboard.app.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * JS-level fetch wrapper using callbacks. CORS failures and network errors are
 * caught in JavaScript and delivered through the onError callback, avoiding
 * unhandled JS promise rejections that crash the Compose coroutine scope in
 * Kotlin/WASM.
 */
@JsFun("""
(url, onSuccess, onError) => {
    fetch(url)
        .then(function(response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status + ': ' + response.statusText);
            }
            return response.text();
        })
        .then(function(text) { onSuccess(text); })
        .catch(function(error) { onError(error.message || 'Network request failed'); });
}
""")
private external fun jsFetchText(url: String, onSuccess: (String) -> Unit, onError: (String) -> Unit)

class NetworkContentServiceWasmImpl : NetworkContentService {

    override suspend fun fetchText(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            jsFetchText(
                url,
                onSuccess = { text -> continuation.resume(text) },
                onError = { message -> continuation.resumeWithException(Exception(message)) },
            )
        }
    }
}
