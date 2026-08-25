package work.bearbrains.joymouse

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import work.bearbrains.joymouse.ui.JoyMouseTheme
import work.bearbrains.joymouse.ui.MainScreen

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
        MainScreen(
          modifier = Modifier.fillMaxSize(),
          onLaunchAccessibilitySettings = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
          },
        )
      }
    }
  }
}
