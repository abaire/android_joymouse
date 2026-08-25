package work.bearbrains.joymouse.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import work.bearbrains.joymouse.MouseAccessibilityService

class ControllerSelectionActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val service = MouseAccessibilityService.instance
    if (service == null) {
      finish()
      return
    }

    val initialControllers = service.getConnectedControllers()
    if (initialControllers.size <= 1) {
      finish()
      return
    }

    setContent {
      JoyMouseTheme {
        var controllers by remember { mutableStateOf(initialControllers) }

        ControllerSelectionDialog(
          controllers = controllers,
          onControllerSelected = { selectedId ->
            service.setPrimaryDevice(selectedId)
            finish()
          },
          onDismiss = {
            finish()
          },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}
