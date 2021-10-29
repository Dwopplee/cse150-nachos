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
		// Ensure atomicity
		lock.acquire();

		// Prevent this speaker from overwriting a previous speaker's word
		while (words) {
			canSpeak.sleep();
		}

		// Once the buffer is empty, add our word to it, then flag it as full
		this.word = word;
		words = true;

		// Wake any waiting listeners
		canListen.wake();
		// Go to sleep to ensure we are partnered before we return
		canSpeak.sleep();

		// Wake the next waiting speaker
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
		// Ensure atomicity
		lock.acquire();

		// Prevent the listener from accessing an empty buffer
		while (!words) {
			canListen.sleep();
		}

		// Once the buffer is full, take the word and flag it as empty
		int message = word;
		words = false;

		// Notify the speaker it is partnered
		// This message may be intercepted by another waiting speaker
		// That's okay -- the speaker will pass it along until it reaches the right one
		canSpeak.wake();

		// We don't have to sleep here -- if we have a word we have a partner

		lock.release();

		return message;
	}

	public static void selfTest() {
	}

	private Lock lock = new Lock();

	private Condition canListen = new Condition(lock);
	private Condition canSpeak = new Condition(lock);

	// Flag to indicate if the buffer is full
	private boolean words = false;

	// Buffer with which to pass messages between speak() and listen()
	private int word;
}
