package com.example.fitness_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.fitness_app.R
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Références aux éléments de l'interface
        logoutButton = view.findViewById(R.id.logout_button)

        // Configuration du bouton Se déconnecter
        logoutButton.setOnClickListener {
            // Déconnexion de Firebase
            FirebaseAuth.getInstance().signOut()

            // Redirection vers le fragment Login
            val loginFragment = LoginFragment() // Instancier le fragment Login
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack(null)  // Facultatif: vous pouvez ajouter à la pile arrière si vous voulez permettre un retour
                .commit()
        }

        return view
    }
}
