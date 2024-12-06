package com.example.fitness_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitness_app.fragments.AddExerciseFragment
import com.example.fitness_app.fragments.CollectionFragment
import com.example.fitness_app.fragments.HomeFragment
import com.example.fitness_app.fragments.LoginFragment
import com.example.fitness_app.fragments.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnProfile: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Calculer le délai initial
        val initialDelay = calculateInitialDelay()

        // Planifier une tâche répétée une fois par jour avec un délai initial
        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS) // Ajouter le délai initial
            .build()

        WorkManager.getInstance(this).enqueue(dailyWorkRequest)

        // Vérifier l'état du réseau au démarrage
        checkNetworkState()

        // Initialisation des éléments
        bottomNavigationView = findViewById(R.id.navigation_view)
        btnProfile = findViewById(R.id.btn_profile)

        // Vérifier si l'activité a été lancée depuis RegisterAccountActivity
        val fromRegisterActivity = intent.getBooleanExtra("FROM_REGISTER_ACTIVITY", false)

        // Si on vient de RegisterAccountActivity, cacher la barre de navigation et le bouton profil
        if (fromRegisterActivity) {
            bottomNavigationView.visibility = View.GONE
            btnProfile.visibility = View.GONE
        }

        // Vérifier si l'utilisateur est connecté
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            loadFragment(HomeFragment())
        } else {
            // Si l'utilisateur n'est pas connecté, afficher LoginFragment
            loadFragment(LoginFragment())
        }

        // Configurer le bouton profil
        btnProfile.setOnClickListener{
            updateNavigationBar2()
            loadFragment(ProfileFragment())
        }

        // Configurer la navigation inférieure
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.home_page -> {
                    loadFragment(HomeFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.collection_page -> {
                    loadFragment(CollectionFragment(this))
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.add_plant_page -> {
                    loadFragment(AddExerciseFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // Injecter le fragment dans le conteneur (fragment_container)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()

        // Gérer la visibilité des éléments en fonction du fragment
        if (fragment is LoginFragment) {
            hideNavigationElements()
        } else {
            showNavigationElements()
        }
    }

    // fonction pour cacher le menu de navigation et le bouton profil
    fun hideNavigationElements() {
        bottomNavigationView.visibility = View.GONE
        btnProfile.visibility = View.GONE
    }

    // fonction pour afficher le menu de navigation et le bouton profil
    fun showNavigationElements() {
        bottomNavigationView.visibility = View.VISIBLE
        btnProfile.visibility = View.VISIBLE
    }

    // fonction pour passer le menu de navigation à "Ajouter"
    fun updateNavigationBar() {
        bottomNavigationView.selectedItemId = R.id.add_plant_page
    }

    // fonction pour passer le menu de navigation à "Profil"
    fun updateNavigationBar2() {
        bottomNavigationView.selectedItemId = R.id.profile
    }

    // fonction pour passer le menu de navigation à "Accueil"
    fun updateNavigationBar3() {
        bottomNavigationView.selectedItemId = R.id.home_page
    }

    private fun checkNetworkState() {
        // Obtenir le gestionnaire de connectivité
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

        // Vérifier si une connexion est active
        if (activeNetwork != null && activeNetwork.isConnected) {
            // Détecter le type de connexion
            if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                Toast.makeText(this, "Connected to Wi-Fi", Toast.LENGTH_LONG).show()
            } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                Toast.makeText(this, "Connected to the mobile network", Toast.LENGTH_LONG).show()
            }
        } else {
            // Pas de connexion réseau
            Toast.makeText(this, "No network connection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Vérifiez si l'autorisation de notification est accordée
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Demander l'autorisation si elle n'est pas encore accordée
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // L'autorisation a été accordée, vous pouvez envoyer des notifications
                    showNotification()
                } else {
                    // L'utilisateur a refusé l'autorisation, vous pouvez afficher un message ou gérer l'échec
                    // comme il n'a pas autorisé l'envoi de notifications
                    // Par exemple : "Les notifications sont désactivées."
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelId = "default_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notificationChannelId, "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, notificationChannelId)
            .setContentTitle("Welcome to Fitness Zone")
            .setContentText("You can now start to enter your workouts.")
            .setSmallIcon(R.drawable.ic_fire)
            .build()

        notificationManager.notify(0, notification)
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val nextRunTime = Calendar.getInstance()

        // Définir l'heure à laquelle la tâche doit s'exécuter (18h ici)
        nextRunTime.set(Calendar.HOUR_OF_DAY, 18)
        nextRunTime.set(Calendar.MINUTE, 0)
        nextRunTime.set(Calendar.SECOND, 0)

        // Si l'heure est déjà passée aujourd'hui, ajouter un jour
        if (nextRunTime.before(currentTime)) {
            nextRunTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Retourner le délai en millisecondes, converti en minutes
        return nextRunTime.timeInMillis - currentTime.timeInMillis
    }

}