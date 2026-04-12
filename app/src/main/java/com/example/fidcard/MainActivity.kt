package com.example.fidcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fidcard.ui.theme.FidCardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FidCardTheme { FidCardNavHost() } }
    }
}

@Composable
fun FidCardNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as FidCardApp

    NavHost(navController = navController, startDestination = "cardList") {
        composable("cardList") {
            // TODO: CardListScreen — placeholder
            Text("Card List")
        }
        composable(
            "cardDetail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            Text("Card Detail")
        }
        composable(
            "cardEdit/{id}?cardNumber={cardNumber}&format={format}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("cardNumber") { type = NavType.StringType; defaultValue = ""; nullable = true },
                navArgument("format")     { type = NavType.StringType; defaultValue = "QR_CODE"; nullable = true }
            )
        ) {
            Text("Card Edit")
        }
        composable("scan") {
            Text("Scan")
        }
    }
}
