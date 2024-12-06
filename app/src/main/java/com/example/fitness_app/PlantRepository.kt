package com.example.fitness_app

import com.example.fitness_app.PlantRepository.Singleton.databaseRef
import com.example.fitness_app.PlantRepository.Singleton.plantList
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PlantRepository {

    object Singleton {
        // Se connecter à la référence "activite"
        val databaseRef = FirebaseDatabase.getInstance().getReference("activite")

        // Créer une liste qui va contenir nos activités
        val plantList = arrayListOf<PlantModel>()
    }

    // Mettre à jour les données depuis Firebase
    fun updateData(callback: () -> Unit) {
        // Absorber les données depuis la databaseRef -> liste d'activités
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Retirer les anciens
                plantList.clear()
                // Récolter la liste
                for (ds in snapshot.children) {
                    // Construire une activité
                    val plant = ds.getValue(PlantModel::class.java)

                    // Vérifier que l'objet n'est pas null
                    if (plant != null) {
                        // Ajouter l'objet
                        plantList.add(plant)
                    }
                }
                // Actionner le callback
                callback()
            }

            override fun onCancelled(error: DatabaseError) {
                // Log ou gérer l'erreur ici
            }
        })
    }

    // Mettre à jour un objet plant en BDD
    fun updatePlant(plant: PlantModel) = databaseRef.child(plant.id).setValue(plant)

    // Insérer une nouvelle activité en BDD
    fun insertPlant(plant: PlantModel) = databaseRef.child(plant.id).setValue(plant)

    // Supprimer une plante de la base
    fun deletePlant(plant: PlantModel) = databaseRef.child(plant.id).removeValue()
}
