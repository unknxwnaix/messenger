package com.example.messenger

import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.messenger.databinding.ActivityMessengerBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

class MessengerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessengerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessengerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation()
        replaceFragment(MessagesFragment())
    }
    // Метод для смены фрагментов
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }

    // Метод для функционала нижней панели навигации
    private fun setupBottomNavigation() {
        binding.bottomNavigationChecker.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.messages_menu -> replaceFragment(MessagesFragment())
                R.id.friends_menu -> replaceFragment(FriendsFragment())
                R.id.profile_menu -> replaceFragment(ProfileFragment())
            }
            true
        }
    }

}