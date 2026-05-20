package com.example.slotmaster.database

import com.example.slotmaster.domain.model.GameHistory
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query

class GameRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun saveGameHistory(
        username: String,
        score: Int,
        result: String,
        symbols: String,
        coinsAfter: Int,
        bet: Int,
        durationSeconds: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Debes iniciar sesión para guardar la partida.")
            return
        }

        val game = hashMapOf(
            "userId" to user.uid,
            "username" to username,
            "symbols" to symbols,
            "score" to score,
            "result" to result,
            "coinsAfter" to coinsAfter,
            "bet" to bet,
            "durationSeconds" to durationSeconds,
            "createdAt" to FieldValue.serverTimestamp(),
            "errorMessage" to null
        )

        db.collection("games")
            .add(game)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(parseFirebaseError(exception))
            }
    }

    fun getGlobalHistory(
        onSuccess: (List<GameHistory>) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Debes iniciar sesión para ver el historial global.")
            return
        }

        db.collection("games")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { result ->
                val games = result.toObjects(GameHistory::class.java)
                onSuccess(games)
            }
            .addOnFailureListener { exception ->
                onError(parseFirebaseError(exception))
            }
    }

    private fun parseFirebaseError(exception: Exception): String {
        return when (exception) {
            is FirebaseNetworkException -> {
                "No hay conexión a internet. Revisa tu red e inténtalo de nuevo."
            }

            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "No tienes permisos para acceder a estos datos."

                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Firebase no está disponible ahora mismo. Inténtalo más tarde."

                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "La operación ha tardado demasiado. Revisa tu conexión."

                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        "No se han encontrado los datos solicitados."

                    else ->
                        "Error de Firebase: ${exception.message ?: "Error desconocido"}"
                }
            }

            else -> {
                exception.message ?: "Ha ocurrido un error inesperado."
            }
        }
    }
}