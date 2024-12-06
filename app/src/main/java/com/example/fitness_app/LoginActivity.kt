package com.example.fitness_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_login)

        // Initialiser FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Références aux éléments de la vue
        val loginButton = findViewById<Button>(R.id.login_button)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val createAccountButton = findViewById<Button>(R.id.create_account_button)

        // Gérer l'événement du bouton de connexion
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Tentative de connexion avec Firebase Authentication
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Connexion réussie
                            val user = auth.currentUser
                            Toast.makeText(
                                this,
                                "Welcome ${user?.email}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Rediriger vers MainActivity
                            redirectToMainActivity()
                        } else {
                            // Connexion échouée
                            Toast.makeText(
                                this,
                                "Login failed. Check your information.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter an email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Gérer l'événement du bouton "Créer un compte"
        createAccountButton.setOnClickListener {
            // Naviguer vers l'activité de création de compte
            val intent = Intent(this, RegisterAccountActivity::class.java)
            startActivity(intent)
        }
    }

    private fun redirectToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Terminer cette activité pour éviter de revenir ici avec le bouton retour
    }
}
