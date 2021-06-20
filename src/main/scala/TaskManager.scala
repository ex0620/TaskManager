package com.iptiq.exercise

import scala.collection.mutable.{ArraySeq, ListBuffer}

class TaskManager (val maxSize: Int = 100) {
  // list of processes
  protected val processes = ListBuffer.empty[Process]
  // free pids. If a pid is used, the corresponding item will be true
  protected val pidStatus = ArraySeq((0 until this.maxSize).map(_ => false):_*)

  protected def addProcess(runnable: Runnable, priority: Priority = Medium): Int = {
    if (processes.size >= this.maxSize) {
      // the TM is full
      -1
    } else {
      val pid = this.pidStatus.indexWhere(!_)
      val process = new Process(runnable, pid, priority)
      processes.append(process)
      this.pidStatus(pid) = true
      pid
    }
  }

  def add(runnable: Runnable, priority: Priority = Medium): Int = {
    this.synchronized {
      this.addProcess(runnable, priority)
    }
  }

  // delete a process
  protected def delete(idx: Int): Unit = {
    val pid = this.processes(idx).pid
    this.processes.remove(idx)
    this.pidStatus(pid) = false
  }

  def addFIFO(runnable: Runnable, priority: Priority = Medium): Int = {
    this.synchronized{
      if (processes.size >= this.maxSize) {
        // delete the oldest process
        this.delete(idx = 0)
      }
      this.addProcess(runnable, priority)
    }
  }

  def addByPriority(runnable: Runnable, priority: Priority = Medium): Int = {
    this.synchronized{
      if (processes.size >= this.maxSize) {
        // delete the oldest process, whose priority is lower
        val idx = this.processes.indexWhere(_.priority < priority)
        if (idx >= 0) {
          this.delete(idx)
        }
      }
      this.addProcess(runnable, priority)
    }
  }

  def kill(pid: Int): Unit = {
    this.synchronized{
      val idx = this.processes.indexWhere(_.pid == pid)
      if (idx >= 0) {
        this.delete(idx)
      }
    }
  }

  protected def deleteAll(): Unit = {
    this.processes.clear()
    for (i <- 0 until this.maxSize) {
      this.pidStatus(i) = false
    }
  }

  def killByPriority(priority: Priority): Unit = {
    this.synchronized{
      val kept = this.processes.filter(_.priority != priority)
      this.deleteAll()

      this.processes ++= kept
      for (p <- kept) {
        this.pidStatus(p.pid) = true
      }
    }
  }

  def killAll(): Unit = {
    this.synchronized{
      this.deleteAll()
    }
  }

  // list running processes ordered by creation time
  def listByCreationTime(desc: Boolean = false): List[Process] = {
    this.synchronized{
      if (!desc) {
        this.processes.toList
      } else {
        this.processes.toList.reverse
      }
    }
  }

  // list running processes ordered by pid
  def listByPid(desc: Boolean = false): List[Process] = {
    this.synchronized{
      this.processes.toList.sortWith((p1, p2) => {
        if (!desc) {
          p1.pid < p2.pid
        } else {
          p1.pid > p2.pid
        }
      })
    }
  }

  // list running processes ordered by priority
  def listByPriority(desc: Boolean = false): List[Process] = {
    this.synchronized{
      this.processes.toList.sortWith((p1, p2) => {
        if (!desc) {
          p1.priority < p2.priority
        } else {
          p1.priority > p2.priority
        }
      })
    }
  }

}
