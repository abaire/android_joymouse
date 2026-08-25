package work.bearbrains.joymouse

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import work.bearbrains.joymouse.ui.JoyMouseTheme
import work.bearbrains.joymouse.ui.MainScreen
import work.bearbrains.joymouse.util.isAccessibilityServiceEnabled

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
      StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
          .detectLeakedClosableObjects()
          .penaltyLog()
          .penaltyDeath()
          .build()
      )
    }

    setContent {
      JoyMouseTheme {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var isServiceEnabled by remember {
          mutableStateOf(isAccessibilityServiceEnabled(context))
        }

        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              isServiceEnabled = isAccessibilityServiceEnabled(context)
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
          }
        }

        MainScreen(
          modifier = Modifier.fillMaxSize(),
          isServiceEnabled = isServiceEnabled,
          onLaunchAccessibilitySettings = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
          },
        )
      }
    }
  }
}
