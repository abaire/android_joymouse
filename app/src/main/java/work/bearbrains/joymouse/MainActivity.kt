package work.bearbrains.joymouse

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      JoyMouseTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Button(
              onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
              }) {
                Text(stringResource(id = R.string.button_open_accessibility_settings))
              }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    startService(Intent(this, MouseAccessibilityService::class.java))
  }
}
