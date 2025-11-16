package com.signlearn.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    private var isGuest = false

    init {
        viewModelScope.launch { emitAuthFromFirebase() }
    }

    private fun emitAuthFromFirebase() {
        val user = auth.currentUser
        _state.value = when {
            isGuest -> AuthState.Guest
            user != null -> AuthState.Authenticated(user)
            else -> AuthState.Unauthenticated
        }
    }

    fun signInAsGuest() {
        isGuest = true
        _state.value = AuthState.Guest
    }

    fun signOut() {
        isGuest = false
        auth.signOut()
        _state.value = AuthState.Unauthenticated
    }

    fun signInWithEmail(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                isGuest = false
                emitAuthFromFirebase()
            } catch (e: Exception) {
                _state.value = AuthState.Error(toUserMessage(AuthAction.Login, e))
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                isGuest = false
                emitAuthFromFirebase()
            } catch (e: Exception) {
                _state.value = AuthState.Error(toUserMessage(AuthAction.Register, e))
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                isGuest = false
                emitAuthFromFirebase()
            } catch (e: Exception) {
                _state.value = AuthState.Error(toUserMessage(AuthAction.Google, e))
            }
        }
    }

    private enum class AuthAction { Login, Register, Google }

    private fun toUserMessage(action: AuthAction, e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode ?: ""
        return when (action) {
            AuthAction.Login -> when (code) {
                "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
                    "El correo o la contraseña no coinciden."
                "ERROR_USER_DISABLED" -> "Tu cuenta está deshabilitada."
                "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Intenta más tarde."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Problema de conexión. Revisa tu internet."
                else -> "No se pudo iniciar sesión. Inténtalo de nuevo."
            }
            AuthAction.Register -> when (code) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Ese correo ya está registrado."
                "ERROR_INVALID_EMAIL" -> "Formato de correo inválido."
                "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil (mín. 6 caracteres)."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Problema de conexión. Revisa tu internet."
                "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Intenta más tarde."
                else -> "No se pudo registrar. Inténtalo de nuevo."
            }
            AuthAction.Google -> when (code) {
                "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                    "Tu correo ya está asociado a otro método. Úsalo para entrar."
                "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                    "La credencial ya está en uso. Prueba a iniciar sesión."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Problema de conexión. Revisa tu internet."
                else -> "No se pudo iniciar con Google. Inténtalo de nuevo."
            }
        }
    }
}
