package work.bearbrains.joymouse.impl

import work.bearbrains.joymouse.NanoClock

class NanoClockImpl : NanoClock {
  override fun nanoTime(): Long = System.nanoTime()
}
