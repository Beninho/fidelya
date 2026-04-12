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
import com.example.fidcard.ui.cardedit.CardEditScreen
import com.example.fidcard.ui.carddetail.CardDetailScreen
import com.example.fidcard.ui.cardlist.CardListScreen
import com.example.fidcard.ui.scan.ScanScreen
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
            CardListScreen(
                repository = app.repository,
                onCardClick = { id -> navController.navigate("cardDetail/$id") },
                onAddClick = { navController.navigate("scan") }
            )
        }
        composable(
            "cardDetail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { back ->
            CardDetailScreen(
                cardId = back.arguments!!.getLong("id"),
                repository = app.repository,
                onEditClick = { id -> navController.navigate("cardEdit/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "cardEdit/{id}?cardNumber={cardNumber}&format={format}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("cardNumber") { type = NavType.StringType; defaultValue = ""; nullable = true },
                navArgument("format") { type = NavType.StringType; defaultValue = "QR_CODE"; nullable = true }
            )
        ) { back ->
            CardEditScreen(
                cardId = back.arguments!!.getLong("id"),
                prefilledCardNumber = back.arguments?.getString("cardNumber")?.ifBlank { null },
                prefilledFormat = back.arguments?.getString("format"),
                repository = app.repository,
                onSaved = { navController.popBackStack("cardList", false) },
                onBack = { navController.popBackStack() }
            )
        }
        composable("scan") {
            ScanScreen(
                onBarcodeDetected = { number, format ->
                    navController.navigate("cardEdit/-1?cardNumber=$number&format=$format") {
                        popUpTo("scan") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
