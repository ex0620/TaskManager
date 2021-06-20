package com.iptiq.exercise

import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.MustMatchers.{convertToAnyMustWrapper, equal}
import org.scalatest.FunSuite
import org.scalatest.concurrent.Conductors
import scala.collection.mutable.{ListBuffer, Set}

class TaskManagerTest extends FunSuite with Conductors  {
  test("Adding a process") {
    val conductor = new Conductor
    import conductor._

    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)
    val pids1 = ListBuffer.empty[Int]
    val pids2 = ListBuffer.empty[Int]
    val pids3 = ListBuffer.empty[Int]

    threadNamed("1") {
      for (i <- 0 until maxSize) {
        val pid = taskManager.add(null)
        if (pid >= 0) {
          pids1.append(pid)
        }
      }
    }

    threadNamed("2") {
      for (i <- 0 until maxSize) {
        val pid = taskManager.add(null)
        if (pid >= 0) {
          pids2.append(pid)
        }
      }
    }

    threadNamed("3") {
      for (i <- 0 until maxSize) {
        val pid = taskManager.add(null)
        if (pid >= 0) {
          pids3.append(pid)
        }
      }
    }

    whenFinished {
      // it should have added exactly "maxSize" processes
      val addedNum = pids1.size + pids2.size + pids3.size
      addedNum should equal (maxSize)

      // all pids must be different
      val set = Set.empty[Int]
      set ++= pids1.toSet
      set ++= pids2.toSet
      set ++= pids3.toSet
      set.size should equal (maxSize)
    }

  }

  test("Adding a process FIFO") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)
    val pids = ListBuffer.empty[Int]

    for (i <- 0 until maxSize) {
      taskManager.add(null)
    }

    // always possible to add new processes
    for (i <- 0 until maxSize) {
      val pid = taskManager.addFIFO(null)
      // the oldest pid is reused
      assert(pid == i)
      pids.append(pid)
    }
    // pids must be unique
    pids.toSet.size should equal(maxSize)
  }

  test("Adding a process with higher priority") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)
    val pids = ListBuffer.empty[Int]

    for (i <- 0 until maxSize) {
      taskManager.add(null)
    }

    for (i <- 0 until maxSize) {
      val pid = taskManager.addByPriority(null, Low)
      assert(pid < 0)
    }

    for (i <- 0 until maxSize) {
      val pid = taskManager.addByPriority(null, Medium)
      assert(pid < 0)
    }

    // always possible to add new processes
    for (i <- 0 until maxSize) {
      val pid = taskManager.addByPriority(null, High)
      // the oldest pid is reused
      assert(pid == i)
      pids.append(pid)
    }
    // pids must be unique
    pids.toSet.size should equal(maxSize)
  }

  test("Killing a process by pid") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)

    for (i <- 0 until maxSize) {
      taskManager.add(null)
    }

    taskManager.kill(100)
    assert(taskManager.listByPid().size == maxSize-1)
    taskManager.kill(100)
    assert(taskManager.listByPid().size == maxSize-1)
    val pid = taskManager.add(null)
    assert(pid == 100)

    taskManager.kill(401)
    assert(taskManager.listByPid().size == maxSize)
    val pid1 = taskManager.add(null)
    assert(pid1 < 0)

    taskManager.kill(-5)
    assert(taskManager.listByPid().size == maxSize)
    val pid2 = taskManager.add(null)
    assert(pid2 < 0)
  }

  test("Killing processes by priority") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)
    val pids = ListBuffer.empty[Int]

    val conductor = new Conductor
    import conductor._

    threadNamed("1") {
      for (i <- 0 until 10) {
        taskManager.add(null, Low)
      }
    }

    threadNamed("2") {
      for (i <- 0 until 100) {
        taskManager.add(null, Medium)
      }
    }

    threadNamed("3") {
      for (i <- 0 until 190) {
        taskManager.add(null, High)
      }
    }

    whenFinished {
      taskManager.killByPriority(Low)
      assert(taskManager.listByPid().size == 290)
      taskManager.killByPriority(High)
      assert(taskManager.listByPid().size == 100)

      for (i <- 0 until 201) {
        val pid = taskManager.add(null)
        if (pid >= 0) {
          pids.append(pid)
        }
      }
      pids.size should equal(200)
      taskManager.listByPid().map(_.pid).toSet.size should equal(maxSize)
    }

  }

  test("Killing all processes") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)

    for (i <- 0 until maxSize) {
      taskManager.add(null)
    }
    taskManager.killAll()
    taskManager.listByPid().size should equal(0)

    for (i <- 0 until maxSize) {
      val pid = taskManager.add(null)
      pid should equal(i)
    }
  }

  test("List by pid") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)

    val conductor = new Conductor
    import conductor._

    threadNamed("1") {
      for (i <- 0 until 10) {
        taskManager.add(null, Low)
      }
    }

    threadNamed("2") {
      for (i <- 0 until 100) {
        taskManager.add(null, Medium)
      }
    }

    threadNamed("3") {
      for (i <- 0 until 190) {
        taskManager.add(null, High)
      }
    }

    whenFinished{
      val list = taskManager.listByPid()
      list.size should equal(maxSize)
      for (i <- 0 until maxSize) {
        list(i).pid should equal(i)
      }

      val list2 = taskManager.listByPid(desc = true)
      for (i <- 0 until maxSize) {
        list2(i).pid should equal(maxSize-i-1)
      }
      list2.size should equal(maxSize)
    }
  }

  test("List by priority") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)

    val conductor = new Conductor
    import conductor._

    threadNamed("1") {
      for (i <- 0 until 10) {
        taskManager.add(null, Low)
      }
    }

    threadNamed("2") {
      for (i <- 0 until 100) {
        taskManager.add(null, Medium)
      }
    }

    threadNamed("3") {
      for (i <- 0 until 190) {
        taskManager.add(null, High)
      }
    }

    whenFinished{
      val list = taskManager.listByPriority()
      for (i <- 1 until maxSize) {
        assert(list(i).priority >= list(i-1).priority)
      }
      list.size should equal(maxSize)

      val list2 = taskManager.listByPriority(desc = true)
      for (i <- 1 until maxSize) {
        assert(list2(i).priority <= list2(i-1).priority)
      }
      list2.size should equal(maxSize)
    }
  }

  test("List by creation time") {
    val maxSize = 300
    val taskManager = new TaskManager(maxSize = maxSize)

    val conductor = new Conductor
    import conductor._

    threadNamed("1") {
      for (i <- 0 until 10) {
        taskManager.add(null, Low)
      }
    }

    threadNamed("2") {
      for (i <- 0 until 100) {
        taskManager.add(null, Medium)
      }
    }

    threadNamed("3") {
      for (i <- 0 until 190) {
        taskManager.add(null, High)
      }
    }

    whenFinished {
      val list = taskManager.listByCreationTime()
      for (i <- 1 until maxSize) {
        assert(list(i).creationTime >= list(i - 1).creationTime)
      }
      list.size should equal(maxSize)

      val list2 = taskManager.listByCreationTime(desc = true)
      for (i <- 1 until maxSize) {
        assert(list2(i).creationTime <= list2(i - 1).creationTime)
      }
      list2.size should equal(maxSize)
    }
  }
}
