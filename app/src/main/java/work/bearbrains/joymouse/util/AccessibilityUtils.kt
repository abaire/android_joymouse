package work.bearbrains.joymouse.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import work.bearbrains.joymouse.MouseAccessibilityService

/** Checks if the specified accessibility service is currently enabled in system settings. */
fun isAccessibilityServiceEnabled(
  context: Context,
  serviceClass: Class<out AccessibilityService> = MouseAccessibilityService::class.java,
): Boolean {
  val accessibilityManager =
    context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
  val enabledServices =
    accessibilityManager.getEnabledAccessibilityServiceList(
      AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    ) ?: return false

  val expectedComponentName = ComponentName(context, serviceClass)
  return enabledServices.any { service ->
    val serviceInfo = service.resolveInfo?.serviceInfo
    if (serviceInfo != null) {
      serviceInfo.packageName == expectedComponentName.packageName &&
        serviceInfo.name == expectedComponentName.className
    } else {
      val serviceId = service.id
      serviceId != null &&
        (serviceId == expectedComponentName.flattenToString() ||
          serviceId == expectedComponentName.flattenToShortString())
    }
  }
}
