package com.example.slotmaster.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.slotmaster.R
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            signIn()
        }
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken == null) {
                Log.e("LOGIN", "ID TOKEN NULL")
                Toast.makeText(
                    this,
                    "Error: token de Google nulo",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }

            Log.d("LOGIN", "ID TOKEN OK")
            firebaseAuthWithGoogle(idToken)

        } catch (e: ApiException) {
            Log.e("LOGIN", "Google SignIn ERROR: ${e.statusCode}", e)

            Toast.makeText(
                this,
                "Error Google Sign-In: ${e.statusCode}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e("LOGIN", "Google SignIn ERROR", e)

            Toast.makeText(
                this,
                "Error al iniciar sesión con Google",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun signIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    Log.d("LOGIN", "Firebase LOGIN OK")

                    Toast.makeText(
                        this,
                        "Login correcto",
                        Toast.LENGTH_SHORT
                    ).show()

                    goToMain()

                } else {
                    Log.e("LOGIN", "Firebase LOGIN FAIL", task.exception)

                    Toast.makeText(
                        this,
                        "Error Firebase: ${task.exception?.message ?: "desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}