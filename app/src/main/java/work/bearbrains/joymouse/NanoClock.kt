package work.bearbrains.joymouse

/** Clock providing a nanosecond precision clock that may be used to measure differences in time. */
interface NanoClock {
  /** Returns the current clock value in nanoseconds. */
  fun nanoTime(): Long
}
