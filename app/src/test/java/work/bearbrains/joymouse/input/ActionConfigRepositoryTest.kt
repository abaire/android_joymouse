package work.bearbrains.joymouse.input

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionConfigRepositoryTest {

  private lateinit var context: Context
  private lateinit var repository: ActionConfigRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences(ActionConfigRepository.PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .clear()
      .commit()
    repository = ActionConfigRepository(context)
  }

  @Test
  fun getConfig_whenEmpty_returnsDefault() {
    val config = repository.getConfig()
    assertThat(config).isEqualTo(ActionConfig.DEFAULT)
  }

  @Test
  fun saveConfig_andGetConfig_returnsSavedConfig() {
    val custom =
      ActionConfig(
        actionBindings =
          mapOf(
            JoystickAction.BACK to
              ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_Y))
          ),
        toggleChord = setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B),
        shiftButton = KeyEvent.KEYCODE_BUTTON_L1,
        altButton = KeyEvent.KEYCODE_BUTTON_R1,
      )

    repository.saveConfig(custom)

    val loaded = repository.getConfig()
    assertThat(loaded.actionBindings[JoystickAction.BACK]?.modifier).isEqualTo(ShiftModifier.SHIFT)
    assertThat(loaded.actionBindings[JoystickAction.BACK]?.keyCodes).containsExactly(KeyEvent.KEYCODE_BUTTON_Y)
    assertThat(loaded.toggleChord).containsExactly(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B)
    assertThat(loaded.shiftButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_L1)
    assertThat(loaded.altButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_R1)
  }

  @Test
  fun resetToDefaults_restoresDefaultConfig() {
    val custom =
      ActionConfig(
        actionBindings =
          mapOf(
            JoystickAction.BACK to
              ActionBinding(ShiftModifier.SHIFT, setOf(KeyEvent.KEYCODE_BUTTON_Y))
          ),
        shiftButton = KeyEvent.KEYCODE_BUTTON_L1,
        altButton = KeyEvent.KEYCODE_BUTTON_R1,
      )
    repository.saveConfig(custom)
    assertThat(repository.getConfig().actionBindings[JoystickAction.BACK]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_Y)

    repository.resetToDefaults()

    val resetConfig = repository.getConfig()
    assertThat(resetConfig.actionBindings[JoystickAction.BACK]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B)
    assertThat(resetConfig.shiftButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_L2)
    assertThat(resetConfig.altButton).isEqualTo(KeyEvent.KEYCODE_BUTTON_R2)
  }

  @Test
  fun listener_isNotifiedOnSave() {
    val observedConfigs = mutableListOf<ActionConfig>()
    val closeable = repository.registerListener { observedConfigs.add(it) }

    // Initial config received
    assertThat(observedConfigs).hasSize(1)
    assertThat(observedConfigs.first()).isEqualTo(ActionConfig.DEFAULT)

    val custom =
      ActionConfig(
        actionBindings =
          mapOf(
            JoystickAction.HOME to
              ActionBinding(ShiftModifier.ALT, setOf(KeyEvent.KEYCODE_BUTTON_X))
          ),
      )
    repository.saveConfig(custom)

    assertThat(observedConfigs).hasSize(2)
    assertThat(observedConfigs.last().actionBindings[JoystickAction.HOME]?.keyCodes)
      .containsExactly(KeyEvent.KEYCODE_BUTTON_X)

    closeable.close()

    repository.saveConfig(ActionConfig.DEFAULT)
    assertThat(observedConfigs).hasSize(2)
  }
}
