package pt.isel.pc.sketches

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class SimpleThreadPool(
    private val maxThreads: Int
) {

    private val lock = ReentrantLock();
    private val workItems = NodeLinkedList<Runnable>()
    private var nOfWorkerThreads = 0

    fun execute(workItem: Runnable): Unit = lock.withLock {
        if (nOfWorkerThreads < maxThreads) {
            // create a new worker
            nOfWorkerThreads += 1
            Thread {
                workerLoop(workItem)

            }.start()
        } else {
            workItems.enqueue(workItem)
        }
    }

    private fun getNextWorkItem(): GetWorkItemResult = lock.withLock {
        return if (workItems.notEmpty) {
            GetWorkItemResult.WorkItem(workItems.pull().value)
        } else {
            nOfWorkerThreads -= 1
            GetWorkItemResult.Exit
        }
    }

    sealed class GetWorkItemResult {
        object Exit : GetWorkItemResult()
        class WorkItem(val workItem: Runnable) : GetWorkItemResult()
    }

    // Does NOT hold the lock
    private fun workerLoop(firstRunnable: Runnable) {
        var currentRunnable = firstRunnable
        while (true) {
            currentRunnable.run()
            currentRunnable = when(val result = getNextWorkItem()) {
                is GetWorkItemResult.WorkItem -> result.workItem
                GetWorkItemResult.Exit -> return
            }
        }
    }
}