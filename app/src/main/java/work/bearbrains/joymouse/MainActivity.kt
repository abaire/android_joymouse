package work.bearbrains.joymouse

import android.content.Intent
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

  override fun onResume() {
    super.onResume()
    startService(Intent(this, MouseAccessibilityService::class.java))
  }
}
