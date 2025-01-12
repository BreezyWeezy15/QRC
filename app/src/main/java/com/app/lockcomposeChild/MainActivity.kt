package com.app.lockcomposeChild


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.lockcomposeChild.screens.ChildScreen
import com.app.lockcomposeChild.screens.CustomScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "child") {
                composable("custom"){
                    CustomScreen(navController)
                }
                composable("child"){
                    ChildScreen(navController = navController)
                }
            }
        }
    }

}


