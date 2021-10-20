package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;

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
		// We don't lock before accessing the queue because this is an interrupt, not a
		// thread
		while (waitQueue.size() > 0 && waitQueue.first().wakeTime < Machine.timer().getTime())
			waitQueue.pollFirst().wake.V();

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
			// Ensure only one thread may access the queue at once
			lock.acquire();

			// Otherwise, package the information into a structure,
			WakeTimer wakeTimer = new WakeTimer(wakeTime);
			// put it in the queue,
			waitQueue.add(wakeTimer);
			// and go to sleep.
			wakeTimer.wake.P();

			lock.release();
		}

	}

	// Add Alarm testing code to the Alarm class

	public static void alarmTest1() {
		int durations[] = { 1000, 1001, 1002, 10 * 1000, 10 * 1001, 10 * 1002, 100 * 1000 };
		long t0, t1;

		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil(d);
			t1 = Machine.timer().getTime();
			System.out.println("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}

	// Implement more test methods here ...

	// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
	public static void selfTest() {
		alarmTest1();

		// Invoke your other test methods here ...
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

	// Lock to ensure mutual exclusion when accessing the queue
	private Lock lock = new Lock();

	// Efficient queue to track the threads we need to wake.
	private TreeSet<WakeTimer> waitQueue = new TreeSet();
}
