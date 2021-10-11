package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();

		if (words) {
			canSpeak.sleep();
		}

		this.word = word;

		words = true;

		canListen.wake();
		canSpeak.sleep();
		canSpeak.wake();

		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();

		if (!words) {
			canListen.sleep();
		}

		int message = word;

		words = false;

		canSpeak.wake();

		lock.release();

		return message;
	}

	private Lock lock = new Lock();

	private Condition canListen = new Condition(lock);
	private Condition canSpeak = new Condition(lock);
	
	private boolean words = false;

	private int word;
}
