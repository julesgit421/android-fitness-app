package com.example.fitness_app.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.fitness_app.MainActivity
import com.example.fitness_app.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CollectionFragment(
    private val context: MainActivity
) : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var exerciseListLayout: LinearLayout
    val exercises = mutableListOf<Pair<Date, String>>() // Liste pour stocker les exercices avec leurs dates

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_collection, container, false)

        // Initialisation de Firebase
        database = FirebaseDatabase.getInstance().getReference("Activites")
        exerciseListLayout = view.findViewById(R.id.exercise_list_layout)

        // Récupérer les références des graphiques
        val lineChartTime = view.findViewById<LineChart>(R.id.line_chart_time)
        val barChartTypes = view.findViewById<BarChart>(R.id.bar_chart_types)
        val lineChartDistance = view.findViewById<LineChart>(R.id.line_chart_distance) // Nouveau graphique de distance

        // Charger les données pour chaque graphique
        loadExerciseData(lineChartTime) // Temps d'exercice
        loadExerciseDataForToday(barChartTypes) // Types d'exercice effectués aujourd'hui
        loadExerciseDataForDistance(lineChartDistance) // Distance parcourue par jour
        loadExercises() // liste des exercices

        return view
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
                    val timeDataSet = LineDataSet(timeEntries, "Exercise time (min)")
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


    // Graphique à barres verticales pour afficher les types d'exercices effectués
    private fun loadExerciseDataForToday(barChart: BarChart) {
        val entries = mutableListOf<BarEntry>()
        val typeCount = mutableMapOf<String, Int>()
        val currentDate = getCurrentDate()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Récupérer l'ID de l'utilisateur

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        for (idSnapshot in categorySnapshot.children){
                            for (exerciseSnapshot in idSnapshot.children) {
                                val date =
                                    exerciseSnapshot.child("jour").getValue(String::class.java)
                                val type =
                                    exerciseSnapshot.child("type").getValue(String::class.java)
                                val exerciseUserId =
                                    exerciseSnapshot.child("userId").getValue(String::class.java)

                                // Filtrer les exercices pour l'utilisateur connecté
                                if (type != null && exerciseUserId == userId) {
                                    typeCount[type] = typeCount.getOrDefault(type, 0) + 1
                                }
                            }
                        }
                    }

                    var index = 0f
                    for ((type, count) in typeCount) {
                        entries.add(BarEntry(index, count.toFloat()))
                        index += 1f
                    }

                    val barDataSet = BarDataSet(entries, "Exercises performed")
                    barDataSet.color = Color.DKGRAY
                    barDataSet.valueTextColor = Color.BLACK
                    val barData = BarData(barDataSet)
                    barChart.data = barData

                    // Configuration des axes du graphique des types d'exercices
                    configureBarChartAxes(barChart, typeCount.keys.toList())
                    barChart.invalidate()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Gérer l'erreur
            }
        })
    }


    private fun formatDate(date: String): String {
        val parts = date.split("-")
        return "${parts[2]}/${parts[1]}"  // Format jj/mm
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun configureAxes(lineChart: LineChart, dateLabels: List<String>) {
        lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dateLabels.getOrNull(value.toInt()) ?: ""
                }
            }
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)  // Retirer le quadrillage
        }
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            setDrawGridLines(false)  // Retirer le quadrillage
        }
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.description.text = ""  // Retirer la description
    }

    private fun configureBarChartAxes(barChart: BarChart, labels: List<String>) {
        barChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return labels.getOrNull(value.toInt()) ?: ""
                }
            }
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)  // Retirer le quadrillage
        }
        barChart.axisLeft.apply {
            axisMinimum = 0f
            setDrawGridLines(false)  // Retirer le quadrillage
        }
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.description.text = ""  // Retirer la description
    }

    private fun loadExerciseDataForDistance(lineChart: LineChart) {
        val distanceEntries = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()
        val distanceByDate = mutableMapOf<String, Float>()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NewApi")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        for (idSnapshot in categorySnapshot.children){
                            for (exerciseSnapshot in idSnapshot.children) {
                                val distance =
                                    exerciseSnapshot.child("distance").getValue(String::class.java)
                                        ?.toFloatOrNull()
                                val date =
                                    exerciseSnapshot.child("jour").getValue(String::class.java)
                                val type =
                                    exerciseSnapshot.child("type").getValue(String::class.java)
                                val exerciseUserId =
                                    exerciseSnapshot.child("userId").getValue(String::class.java)

                                // Ajouter la distance uniquement pour les exercices cardio
                                if (distance != null && !date.isNullOrEmpty() && type != null && (type.equals(
                                        "running",
                                        ignoreCase = true
                                    ) || type.equals("walking", ignoreCase = true)) && exerciseUserId == userId
                                ) {

                                    distanceByDate[date] =
                                        distanceByDate.getOrDefault(date, 0f) + distance
                                }
                            }
                        }
                    }

                    // Trier les dates par ordre croissant
                    val sortedDates = distanceByDate.keys.sorted()

                    // Ajouter les données au graphique de distance
                    var index = 0f
                    for (date in sortedDates) {
                        val totalDistance = distanceByDate[date] ?: 0f
                        distanceEntries.add(Entry(index, totalDistance))
                        dateLabels.add(formatDate(date))
                        index += 1f
                    }

                    // Configuration du graphique de distance
                    val distanceDataSet = LineDataSet(distanceEntries, "Distance traveled (km)")
                    distanceDataSet.color = Color.DKGRAY
                    distanceDataSet.valueTextColor = Color.BLACK
                    distanceDataSet.lineWidth = 2f
                    val distanceLineData = LineData(distanceDataSet)
                    lineChart.data = distanceLineData

                    // Configuration des axes du graphique de distance
                    configureAxesForDistance(lineChart, dateLabels)

                    // Rafraîchir le graphique
                    lineChart.invalidate()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestion des erreurs
            }
        })
    }

    private fun configureAxesForDistance(lineChart: LineChart, dateLabels: List<String>) {
        lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dateLabels.getOrNull(value.toInt()) ?: ""
                }
            }
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)  // Retirer le quadrillage
        }
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            setDrawGridLines(false)  // Retirer le quadrillage
        }
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.description.text = ""  // Retirer la description
    }

    // Afficher la liste des exercices effectués
    private fun loadExercises() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.addListenerForSingleValueEvent(object : ValueEventListener {
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
                // Gérer l'erreur (si nécessaire)
            }
        })
    }
}
