package com.example.fitness_app.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitness_app.R
import com.example.fitness_app.Exercise
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

class AddExerciseFragment : Fragment() {

    // Référence Firebase
    private lateinit var database: DatabaseReference
    private lateinit var database2: DatabaseReference

    private lateinit var exerciseListLayout: LinearLayout
    val exercises = mutableListOf<Pair<Date, String>>() // Liste pour stocker les exercices avec leurs dates

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_exercise, container, false)

        // Initialisation de Firebase
        database = FirebaseDatabase.getInstance().reference  // Initialisation correcte avec DatabaseReference
        database2 = FirebaseDatabase.getInstance().getReference("Activites")
        exerciseListLayout = view.findViewById(R.id.exercise_list_layout)

        // Références aux éléments de la vue
        val confirmButton = view.findViewById<Button>(R.id.confirm_button)
        val exerciseTypeGroup = view.findViewById<RadioGroup>(R.id.exercise_type_group)
        val staticDetailsLayout = view.findViewById<LinearLayout>(R.id.static_details_layout)
        val cardioDetailsLayout = view.findViewById<LinearLayout>(R.id.cardio_details_layout)
        val exerciseSpinner = view.findViewById<Spinner>(R.id.exercise_spinner)
        val seriesInput = view.findViewById<EditText>(R.id.series_input)
        val repetitionsInput = view.findViewById<EditText>(R.id.repetitions_input)
        val staticTimeInput = view.findViewById<EditText>(R.id.static_time_input)
        val distanceInput = view.findViewById<EditText>(R.id.distance_input)
        val cardioTimeInput = view.findViewById<EditText>(R.id.cardio_time_input)

        // Fonction pour mettre à jour les options du Spinner
        fun updateSpinnerOptions(options: Array<String>) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            exerciseSpinner.adapter = adapter
        }

        // Mettre à jour les options du Spinner en fonction du type d'exercice sélectionné
        exerciseTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.static_exercise_option -> {
                    // Masquer les détails cardio et afficher les détails statiques
                    staticDetailsLayout.visibility = View.VISIBLE
                    cardioDetailsLayout.visibility = View.GONE
                    val staticExercises = arrayOf("Push-ups", "Abs", "Pull-ups")
                    updateSpinnerOptions(staticExercises)
                    exerciseSpinner.visibility = View.VISIBLE
                }
                R.id.cardio_exercise_option -> {
                    // Masquer les détails statiques et afficher les détails cardio
                    staticDetailsLayout.visibility = View.GONE
                    cardioDetailsLayout.visibility = View.VISIBLE
                    val cardioExercises = arrayOf("Running", "Walking")
                    updateSpinnerOptions(cardioExercises)
                    exerciseSpinner.visibility = View.VISIBLE
                }
            }
        }

        confirmButton.setOnClickListener {

            // Vérifier si une option est sélectionnée
            val selectedRadioButtonId = exerciseTypeGroup.checkedRadioButtonId
            if (selectedRadioButtonId == -1) {
                // Aucune option sélectionnée
                Toast.makeText(context, "Select an exercise.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Récupération de l'exercice sélectionné
            val id = UUID.randomUUID().toString()  // Générer un ID unique
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val selectedExercise = exerciseSpinner.selectedItem.toString()

            // Récupérer l'ID utilisateur (assurez-vous que l'utilisateur est authentifié)
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId == null) {
                Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var isSuccess = false

            // Enregistrer dans Firebase en fonction du type d'exercice
            if (exerciseTypeGroup.checkedRadioButtonId == R.id.static_exercise_option) {
                val series = seriesInput.text.toString()
                val repetitions = repetitionsInput.text.toString()
                val time = staticTimeInput.text.toString()

                if (series.isNotEmpty() && repetitions.isNotEmpty() && time.isNotEmpty()) {
                    val staticExercise = StaticExercise(
                        id = id,
                        jour = date,
                        type = selectedExercise,
                        serie = series,
                        repetitions = repetitions,
                        temps = time,
                        userId = userId  // Ajout de l'ID utilisateur
                    )

                    // Enregistrer dans Firebase sous "Activites/Statique/id"
                    database.child("Activites").child("Statique").child(userId).child(id).setValue(staticExercise)
                    isSuccess = true
                }
            } else if (exerciseTypeGroup.checkedRadioButtonId == R.id.cardio_exercise_option) {
                val distance = distanceInput.text.toString()
                val time = cardioTimeInput.text.toString()

                if (distance.isNotEmpty() && time.isNotEmpty()) {
                    val cardioExercise = CardioExercise(
                        id = id,
                        jour = date,
                        type = selectedExercise,
                        distance = distance,
                        temps = time,
                        userId = userId  // Ajout de l'ID utilisateur
                    )

                    // Enregistrer dans Firebase sous "Activites/Cardio/id"
                    database.child("Activites").child("Cardio").child(userId).child(id).setValue(cardioExercise)
                    isSuccess = true
                }
            }

            // Affichage d'un message de validation
            if (isSuccess) {
                Toast.makeText(context, "Exercise successfully added !", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Complete all required fields.", Toast.LENGTH_SHORT).show()
            }

            // Effacer les champs après ajout
            seriesInput.text.clear()
            repetitionsInput.text.clear()
            staticTimeInput.text.clear()
            distanceInput.text.clear()
            cardioTimeInput.text.clear()
            exerciseSpinner.setSelection(0)  // Réinitialiser le Spinner
        }

        loadExercises() // liste des exercices

        return view
    }

    private fun loadExercises() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    exerciseListLayout.removeAllViews()  // Vider les vues précédentes

                    // Parcourir les données et ajouter chaque exercice dans le LinearLayout
                    for (categorySnapshot in snapshot.children) {
                        for (idSnapshot in categorySnapshot.children) {
                            for (exerciseSnapshot in idSnapshot.children) {
                                val exerciseType =
                                    exerciseSnapshot.child("type").getValue(String::class.java)
                                val exerciseTime =
                                    exerciseSnapshot.child("temps").getValue(String::class.java)
                                val exerciseDay =
                                    exerciseSnapshot.child("jour").getValue(String::class.java)
                                val exerciseDistance =
                                    exerciseSnapshot.child("distance").getValue(String::class.java)
                                val exerciseSeries =
                                    exerciseSnapshot.child("serie").getValue(String::class.java)
                                val exerciseRep =
                                    exerciseSnapshot.child("repetitions").getValue(String::class.java)
                                val exerciseId =
                                    exerciseSnapshot.child("userId").getValue(String::class.java)

                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = exerciseDay?.let { dateFormat.parse(it) }
                                var details = ""

                                if(exerciseDistance == null){
                                    details = "$exerciseType - $exerciseSeries sets - $exerciseRep repetitions - $exerciseTime min - $exerciseDay"
                                }
                                else{
                                    details = "$exerciseType - $exerciseDistance km - $exerciseTime min - $exerciseDay"
                                }

                                if (exerciseId == userId) {
                                    exercises.add(Pair(date, details) as Pair<Date, String>)
                                }
                            }
                        }
                    }

                    // Trier les exercices par date (du plus récent au plus ancien)
                    exercises.sortByDescending { it.first }

                    for (exercise in exercises){

                        // Créer un LinearLayout pour chaque exercice
                        val exerciseLayout = LinearLayout(requireContext())
                        exerciseLayout.orientation = LinearLayout.VERTICAL
                        exerciseLayout.setPadding(10, 10, 10, 10)
                        exerciseLayout.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                        // Créer un TextView pour chaque exercice
                        val exerciseTextView = TextView(requireContext())

                        exerciseTextView.text =
                            exercise.second
                        exerciseTextView.setPadding(10, 10, 10, 10)
                        exerciseTextView.setTextColor(Color.BLACK)
                        exerciseTextView.textSize = 15f

                        // Ajouter un trait séparateur entre chaque exercice
                        val separator = View(requireContext())
                        separator.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )
                        separator.setBackgroundColor(Color.BLACK)

                        // Ajouter le trait et l'exerciceLayout à l'exercice list
                        exerciseListLayout.addView(exerciseLayout)
                        exerciseListLayout.addView(separator)
                        exerciseLayout.addView(exerciseTextView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }





}

// Data classes pour les exercices
data class StaticExercise(
    val id: String,
    val jour: String,
    val type: String,
    val serie: String,
    val repetitions: String,
    val temps: String,
    val userId: String // Ajout du champ userId
)

data class CardioExercise(
    val id: String,
    val jour: String,
    val type: String,
    val distance: String,
    val temps: String,
    val userId: String // Ajout du champ userId
)
