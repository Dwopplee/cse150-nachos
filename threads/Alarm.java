package nachos.threads;

import nachos.machine.*;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current thread
	 * to yield, forcing a context switch if there is another thread that should be
	 * run.
	 */
	public void timerInterrupt() {
		// Front of the queue will always be the thread with the smallest wait time
		// If we're past the wake time, wake the thread, then check the next one
		while (waitQueue.peek() != null && waitQueue.peek().wakeTime < Machine.timer().getTime())
			waitQueue.poll().wake.V();
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up in
	 * the timer interrupt handler. The thread must be woken up (placed in the
	 * scheduler ready set) during the first timer interrupt where
	 *
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 *
	 * @param x the minimum number of clock ticks to wait.
	 *
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// Get an absolute time at which to wake the thread
		long wakeTime = Machine.timer().getTime() + x;

		// If the time has already passed we can just return
		if (wakeTime > Machine.timer().getTime()) {
			// Otherwise, package the information into a structure,
			WakeTimer wakeTimer = new WakeTimer(wakeTime);
			// put it in the queue,
			waitQueue.add(wakeTimer);
			// and go to sleep.
			wakeTimer.wake.P();
		}

	}

	public static void selfTest() {
	}

	/**
	 * Simple data structure to track threads and their wake times
	 * 
	 * Define the wake time upon initialization, and the structure will
	 * automatically create a semaphore to sleep and wake the thread with. Needs to
	 * be comparable for compatibility with Java's priority queue. Comparison simply
	 * compares the values of wakeTime.
	 */
	private class WakeTimer implements Comparable<WakeTimer> {
		public WakeTimer(long wakeTime) {
			this.wakeTime = wakeTime;
			wake = new Semaphore(0);
		}

		public int compareTo(WakeTimer o) {
			return (int) (this.wakeTime - o.wakeTime);
		}

		public Semaphore wake;
		public long wakeTime;
	}

	// Thread-safe queue to track the threads we need to wake.
	private PriorityBlockingQueue<WakeTimer> waitQueue = new PriorityBlockingQueue();
}
