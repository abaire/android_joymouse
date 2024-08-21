package work.bearbrains.joymouse.test

import work.bearbrains.joymouse.NanoClock

class FakeClock : NanoClock {
  var timestamp = 100L

  override fun nanoTime(): Long = timestamp

  fun advanceMilliseconds(duration: Number) {
    timestamp += duration.toLong() * 1_000_000
  }
}
