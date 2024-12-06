package com.example.fitness_app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.example.fitness_app.MainActivity
import com.example.fitness_app.R
import com.example.fitness_app.RegisterAccountActivity
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Initialiser FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Références aux éléments de la vue
        val loginButton = view.findViewById<Button>(R.id.login_button)
        val emailInput = view.findViewById<EditText>(R.id.email_input)
        val passwordInput = view.findViewById<EditText>(R.id.password_input)
        val createAccountButton = view.findViewById<Button>(R.id.create_account_button)

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
                            Toast.makeText(context, "Welcome ${user?.email}", Toast.LENGTH_SHORT).show()

                            // Rediriger vers HomeFragment
                            redirectToHomeFragment()
                        } else {
                            // Connexion échouée
                            Toast.makeText(context, "Login failed. Check your information.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Please enter an email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Gérer l'événement du bouton "Créer un compte"
        createAccountButton.setOnClickListener {
            // Naviguer vers l'activité de création de compte
            val intent = Intent(activity, RegisterAccountActivity::class.java)
            startActivity(intent)
        }

        // Appel à la méthode pour cacher les éléments
        (activity as MainActivity).hideNavigationElements()

        return view
    }

    private fun redirectToHomeFragment() {
        val homeFragment = HomeFragment() // Instanciation du nouveau fragment
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        // Remplacement du fragment actuel par HomeFragment
        fragmentTransaction.replace(R.id.fragment_container, homeFragment)
        fragmentTransaction.addToBackStack(null) // Permet de revenir en arrière si nécessaire
        fragmentTransaction.commit()
    }
}