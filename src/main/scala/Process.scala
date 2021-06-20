package com.iptiq.exercise

//ordered priorities
sealed abstract class Priority(val order: Int) extends Ordered[Priority] {
  def compare(that: Priority): Int = this.order - that.order
}
case object Low extends Priority(order = 1)
case object Medium extends Priority(order = 2)
case object High extends Priority(order = 3)

class Process (val executable: Runnable, val pid: Int, val priority: Priority = Medium) {
  val creationTime = System.currentTimeMillis()
}
