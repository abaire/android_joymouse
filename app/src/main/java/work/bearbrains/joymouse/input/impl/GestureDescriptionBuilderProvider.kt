package work.bearbrains.joymouse.input.impl

import android.accessibilityservice.GestureDescription
import javax.inject.Provider

/** Provides instances of [GestureDescription.Builder]. */
internal object GestureDescriptionBuilderProvider : Provider<GestureDescription.Builder> {
  override fun get(): GestureDescription.Builder = GestureDescription.Builder()
}
