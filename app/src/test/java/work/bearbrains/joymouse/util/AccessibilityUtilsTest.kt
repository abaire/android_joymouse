package work.bearbrains.joymouse.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import work.bearbrains.joymouse.MouseAccessibilityService

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AccessibilityUtilsTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun isAccessibilityServiceEnabled_whenServiceNotEnabled_returnsFalse() {
    val result = isAccessibilityServiceEnabled(context, MouseAccessibilityService::class.java)
    assertThat(result).isFalse()
  }

  @Test
  fun isAccessibilityServiceEnabled_whenServiceEnabled_returnsTrue() {
    val accessibilityManager =
      context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val shadowAm = shadowOf(accessibilityManager)

    val serviceInfo =
      createAccessibilityServiceInfo(
        packageName = context.packageName,
        className = MouseAccessibilityService::class.java.name,
      )

    shadowAm.setEnabledAccessibilityServiceList(listOf(serviceInfo))

    val result = isAccessibilityServiceEnabled(context, MouseAccessibilityService::class.java)
    assertThat(result).isTrue()
  }

  @Test
  fun isAccessibilityServiceEnabled_whenOtherServiceEnabled_returnsFalse() {
    val accessibilityManager =
      context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val shadowAm = shadowOf(accessibilityManager)

    val otherServiceInfo =
      createAccessibilityServiceInfo(
        packageName = "com.other.app",
        className = "com.other.app.OtherService",
      )

    shadowAm.setEnabledAccessibilityServiceList(listOf(otherServiceInfo))

    val result = isAccessibilityServiceEnabled(context, MouseAccessibilityService::class.java)
    assertThat(result).isFalse()
  }

  private fun createAccessibilityServiceInfo(
    packageName: String,
    className: String,
  ): AccessibilityServiceInfo {
    val serviceInfo = AccessibilityServiceInfo()
    val resolveInfo =
      ResolveInfo().apply {
        this.serviceInfo =
          ServiceInfo().apply {
            this.packageName = packageName
            this.name = className
          }
      }
    try {
      val method =
        AccessibilityServiceInfo::class.java.getMethod(
          "setResolveInfo",
          ResolveInfo::class.java,
        )
      method.invoke(serviceInfo, resolveInfo)
    } catch (_: Exception) {
      val field = AccessibilityServiceInfo::class.java.getDeclaredField("mResolveInfo")
      field.isAccessible = true
      field.set(serviceInfo, resolveInfo)
    }
    return serviceInfo
  }
}
