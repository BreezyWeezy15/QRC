package com.app.lockcomposeChild.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase

@Composable
fun StartScreen(navController: NavController) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val firebaseDatabase = FirebaseDatabase.getInstance().getReference().child("Apps")
            .child(generateDeviceID(context))

        firebaseDatabase.child("type").get()
            .addOnSuccessListener { snapshot ->
                val profileType = snapshot.getValue(String::class.java)
                if (profileType != null) {
                    when (profileType) {
                        "Child", "Teen", "Pre-K" -> {
                            navController.navigate("child")
                        }
                        else -> {
                            navController.navigate("custom")
                        }
                    }
                } else {
                    navController.navigate("custom")
                    Toast.makeText(context, "Profile type not found, defaulting to child", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                navController.navigate("custom")
                Toast.makeText(context, "Failed to fetch profile", Toast.LENGTH_SHORT).show()
            }
    }
}


