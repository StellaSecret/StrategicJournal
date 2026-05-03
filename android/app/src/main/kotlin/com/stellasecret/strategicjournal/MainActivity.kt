package com.stellasecret.strategicjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.stellasecret.strategicjournal.presentation.screens.entry.EntryScreen
import com.stellasecret.strategicjournal.presentation.screens.home.HomeScreen
import com.stellasecret.strategicjournal.presentation.screens.review.ReviewScreen
import com.stellasecret.strategicjournal.presentation.theme.StrategicJournalTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import android.util.Log

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val authStateChanged = mutableStateOf(false)
        private const val DRIVE_SCOPE_REQUEST_CODE = 9001
    }

    // Single launcher handles ALL sign-in results (both initial + scope request)
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        Log.d("SJ_AUTH", "signInLauncher result received: resultCode="+result.resultCode)
        handleSignInResult(result)
    }

    private fun handleSignInResult(result: ActivityResult) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val hasScope = GoogleSignIn.hasPermissions(
                account, Scope(DriveScopes.DRIVE_APPDATA)
            )
            Log.d("SJ_AUTH", "Sign-in OK: email="+account.email+", hasDriveScope="+hasScope)

            if (hasScope) {
                Toast.makeText(this, "Connected to Drive ✓", Toast.LENGTH_SHORT).show()
                authStateChanged.value = !authStateChanged.value
            } else {
                // Got account but no Drive scope — request it with requestCode
                Log.d("SJ_AUTH", "Missing Drive scope, requesting via requestCode...")
                GoogleSignIn.requestPermissions(
                    this,
                    DRIVE_SCOPE_REQUEST_CODE,
                    account,
                    Scope(DriveScopes.DRIVE_APPDATA)
                )
            }
        } catch (e: ApiException) {
            Log.e("SJ_AUTH", "Sign-in failed: statusCode="+e.statusCode+", message="+e.message)
            Toast.makeText(this, "Sign-in failed (${e.statusCode})", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DRIVE_SCOPE_REQUEST_CODE) {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            val hasScope = account != null &&
                GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
            Log.d("SJ_AUTH", "Drive scope result: hasScope="+hasScope)
            if (hasScope) {
                Toast.makeText(this, "Connected to Drive ✓", Toast.LENGTH_SHORT).show()
                authStateChanged.value = !authStateChanged.value
            } else {
                Toast.makeText(this, "Drive permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchGoogleSignIn() {
        Log.d("SJ_AUTH", "launchGoogleSignIn called")
        val driveScope = Scope(DriveScopes.DRIVE_APPDATA)
        val existingAccount = GoogleSignIn.getLastSignedInAccount(this)

        if (existingAccount != null && GoogleSignIn.hasPermissions(existingAccount, driveScope)) {
            // Already signed in with Drive scope
            Log.d("SJ_AUTH", "Already authenticated with Drive scope")
            Toast.makeText(this, "Already connected to Drive ✓", Toast.LENGTH_SHORT).show()
            authStateChanged.value = !authStateChanged.value
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()

        Log.d("SJ_AUTH", "Launching sign-in intent...")
        signInLauncher.launch(GoogleSignIn.getClient(this, gso).signInIntent)
    }

    fun launchGoogleSignOut() {
        Log.d("SJ_AUTH", "launchGoogleSignOut called")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut()
            .addOnCompleteListener {
                Log.d("SJ_AUTH", "Sign-out complete")
                Toast.makeText(this, "Disconnected from Drive", Toast.LENGTH_SHORT).show()
                authStateChanged.value = !authStateChanged.value
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrategicJournalTheme {
                val navController = rememberNavController()
                AppNavHost(navController, activity = this)
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val ENTRY = "entry?entryId={entryId}"
    const val REVIEW = "review"
    fun entry(entryId: String? = null) =
        if (entryId != null) "entry?entryId=$entryId" else "entry"
}

@Composable
fun AppNavHost(navController: NavHostController, activity: MainActivity) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToEntry = { id -> navController.navigate(Routes.entry(id)) },
                onNavigateToReview = { navController.navigate(Routes.REVIEW) },
                onGoogleSignIn = { activity.launchGoogleSignIn() },
                onGoogleSignOut = { activity.launchGoogleSignOut() }
            )
        }
        composable(
            route = Routes.ENTRY,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            EntryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REVIEW) {
            ReviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
