package com.app.lockcomposeChild


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.lockcomposeChild.screens.ChildScreen
import com.app.lockcomposeChild.screens.CustomScreen
import com.app.lockcomposeChild.screens.StartScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "main") {
                composable("main"){
                    StartScreen(navController = navController)
                }
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


