package work.bearbrains.joymouse

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import work.bearbrains.joymouse.ui.JoyMouseTheme
import work.bearbrains.joymouse.ui.MainScreen

class MainActivity : ComponentActivity() {

  private val overlayEnabledState = mutableStateOf(false)

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

    overlayEnabledState.value = Settings.canDrawOverlays(this)
    val manageOverlayActivityResult =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          overlayEnabledState.value = Settings.canDrawOverlays(this)
        }
      }

    setContent {
      JoyMouseTheme {
        MainScreen(
          overlayEnabledState.value,
          modifier = Modifier.fillMaxSize(),
          onLaunchAccessibilitySettings = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
          },
          onEnableOverlay = {
            val intent =
              Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
              )
            manageOverlayActivityResult.launch(intent)
          },
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    startService(Intent(this, MouseAccessibilityService::class.java))
  }
}
