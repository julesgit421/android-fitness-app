package com.example.fitness_app

data class Exercise(
    val id: String,
    val type: String,
    val details: String // Ce champ peut contenir des informations spécifiques à l'exercice
)