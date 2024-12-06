package com.example.fitness_app.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.fitness_app.MainActivity
import com.example.fitness_app.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class HomeFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var streakValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialisation des éléments d'interface
        streakValue = view.findViewById(R.id.day_streak_value)

        // Références
        val lineChart = view.findViewById<LineChart>(R.id.line_chart)
        val addExerciseButton = view.findViewById<Button>(R.id.add_exercise_button)

        // Initialisation de Firebase
        database = FirebaseDatabase.getInstance().getReference("Activites")

        // Calcul et affichage du Day Streak
        calculateDayStreak()

        // Charger les données depuis Firebase
        loadExerciseData(lineChart)

        // Configurer le bouton Ajouter des Exercices
        addExerciseButton.setOnClickListener {
            navigateToAddExerciseFragment()
        }

        // Appel à la méthode pour afficher les éléments
        (activity as MainActivity).showNavigationElements()

        return view
    }

    private fun calculateDayStreak() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val exerciseDates = mutableSetOf<String>()

                    // Collecter toutes les dates des exercices
                    for (categorySnapshot in snapshot.children) {
                        for (idSnapshot in categorySnapshot.children){
                            for (exerciseSnapshot in idSnapshot.children) {
                                val date = exerciseSnapshot.child("jour").getValue(String::class.java)
                                val id = exerciseSnapshot.child("userId").getValue(String::class.java)
                                if (!date.isNullOrEmpty() && id == userId) {
                                    exerciseDates.add(date)
                                }
                                }
                        }
                    }

                    // Calculer le Day Streak
                    val streak = computeStreak(exerciseDates)
                    streakValue.text = streak.toString()
                } else {
                    streakValue.text = "0"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                streakValue.text = "0"
            }
        })
    }

    private fun computeStreak(dates: Set<String>): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()
        val exerciseDates = dates.map { dateFormat.parse(it) }.sortedDescending()

        var streak = 0
        var currentDate = today.time

        // Vérification des dates consécutives
        for (exerciseDate in exerciseDates) {
            val difference = (today.timeInMillis - exerciseDate.time) / (1000 * 60 * 60 * 24)

            if (difference == streak.toLong()) {
                streak++
            } else if (difference > streak) {
                break
            }
        }

        return streak
    }

    private fun navigateToAddExerciseFragment() {
        // Naviguer vers le fragment AddPlantFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AddExerciseFragment())
            .addToBackStack(null)
            .commit()
        // Appeler la méthode pour afficher les éléments de la barre de navigation après le changement de fragment
        (activity as MainActivity).updateNavigationBar()
    }

    // Graphique du temps d'exercice en fonction des jours
    private fun loadExerciseData(lineChart: LineChart) {
        val timeEntries = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()
        val exercisesByDate = mutableMapOf<String, Float>()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Récupérer l'ID de l'utilisateur

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        for (idSnapshot in categorySnapshot.children){
                            for (exerciseSnapshot in idSnapshot.children) {
                                val time =
                                    exerciseSnapshot.child("temps").getValue(String::class.java)
                                        ?.toFloatOrNull()
                                val date =
                                    exerciseSnapshot.child("jour").getValue(String::class.java)
                                val exerciseUserId =
                                    exerciseSnapshot.child("userId").getValue(String::class.java)

                                // Filtrer les exercices pour l'utilisateur connecté
                                if (time != null && !date.isNullOrEmpty() && exerciseUserId == userId) {
                                    exercisesByDate[date] =
                                        exercisesByDate.getOrDefault(date, 0f) + time
                                }
                            }
                        }
                    }

                    // Trier les dates par ordre croissant
                    val sortedDates = exercisesByDate.keys.sorted()

                    // Ajouter les données au graphique du temps
                    var index = 0f
                    for (date in sortedDates) {
                        val totalTime = exercisesByDate[date] ?: 0f
                        timeEntries.add(Entry(index, totalTime))
                        dateLabels.add(formatDate(date))
                        index += 1f
                    }

                    // Configuration du graphique du temps
                    val timeDataSet = LineDataSet(timeEntries, "Exercise time")
                    timeDataSet.color = Color.DKGRAY
                    timeDataSet.valueTextColor = Color.BLACK
                    timeDataSet.lineWidth = 2f
                    val timeLineData = LineData(timeDataSet)
                    lineChart.data = timeLineData

                    // Configuration des axes du graphique du temps
                    configureAxes(lineChart, dateLabels)

                    // Rafraîchir le graphique
                    lineChart.invalidate()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestion des erreurs
                println("Database read error : ${error.message}")
            }
        })
    }

    private fun formatDate(date: String): String {
        val parts = date.split("-")
        return "${parts[2]}/${parts[1]}"
    }

    private fun configureAxes(lineChart: LineChart, dateLabels: List<String>) {
        val xAxis = lineChart.xAxis
        xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dateLabels.getOrNull(value.toInt()) ?: ""
                }
            }
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false) // Supprime les lignes verticales
        }

        // Désactiver les lignes horizontales
        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(false) // Supprime les lignes horizontales

        // Désactiver la ligne de droite
        lineChart.axisRight.isEnabled = false

        // Désactiver la légende
        lineChart.legend.isEnabled = false

        // Supprimer la description du graphique
        lineChart.description.text = ""
    }
}
