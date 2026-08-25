package work.bearbrains.joymouse.input

import android.content.Context
import android.content.SharedPreferences

/** Repository for persisting and observing [ActionConfig] changes. */
class ActionConfigRepository(
  context: Context,
  private val sharedPreferences: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
) {

  private val listeners = mutableSetOf<(ActionConfig) -> Unit>()

  private val prefChangeListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == KEY_CONFIG) {
        val config = getConfig()
        synchronized(listeners) {
          listeners.forEach { it(config) }
        }
      }
    }

  init {
    sharedPreferences.registerOnSharedPreferenceChangeListener(prefChangeListener)
  }

  fun getConfig(): ActionConfig {
    val jsonString = sharedPreferences.getString(KEY_CONFIG, null) ?: return ActionConfig.DEFAULT
    return ActionConfig.fromJson(jsonString)
  }

  fun saveConfig(config: ActionConfig) {
    sharedPreferences.edit().putString(KEY_CONFIG, config.toJson()).apply()
  }

  fun resetToDefaults() {
    sharedPreferences.edit().remove(KEY_CONFIG).apply()
  }

  fun registerListener(listener: (ActionConfig) -> Unit): AutoCloseable {
    synchronized(listeners) {
      listeners.add(listener)
    }
    listener(getConfig())
    return AutoCloseable {
      synchronized(listeners) {
        listeners.remove(listener)
      }
    }
  }

  companion object {
    const val PREFS_NAME = "joymouse_action_config"
    const val KEY_CONFIG = "action_config_json"
  }
}
