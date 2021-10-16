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

		while (words) {
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

		while (!words) {
			canListen.sleep();
		}

		int message = word;

		words = false;

		canSpeak.wake();

		lock.release();

		return message;
	}

	// Add communicator testing code to the Communicator class

	/**
	 * Test with 1 listener then 1 speaker.
	 */
	public static void selfTest1() {

		KThread listener1 = new KThread(listenRun);
		listener1.setName("listener1");
		listener1.fork();

		KThread speaker1 = new KThread(speakerRun);
		speaker1.setName("speaker1");
		speaker1.fork();

		listener1.join();
		speaker1.join();

	} // selfTest1()

	/**
	 * Test with 1 speaker then 1 listener.
	 */
	public static void selfTest2() {

		KThread speaker1 = new KThread(speakerRun);
		speaker1.setName("speaker1");
		speaker1.fork();

		KThread listener1 = new KThread(listenRun);
		listener1.setName("listener1");
		listener1.fork();

		listener1.join();
		speaker1.join();

	} // selfTest2()

	/**
	 * Test with 2 speakers and 2 listeners intermixed.
	 */
	public static void selfTest3() {

		KThread speaker1 = new KThread(speakerRun);
		speaker1.setName("speaker1");
		speaker1.fork();

		KThread listener1 = new KThread(listenRun);
		listener1.setName("listener1");
		listener1.fork();

		KThread listener2 = new KThread(listenRun);
		listener2.setName("listener2");
		listener2.fork();

		KThread speaker2 = new KThread(speakerRun);
		speaker2.setName("speaker2");
		speaker2.fork();

		speaker2.join();
		listener2.join();

	} // selfTest3()

	/**
	 * Test with n > 0 speakers and n > 0 listerners
	 */
	public static void selfTest4() {
		int n = 25;
		KThread speaker = new KThread();
		KThread listener = new KThread();;
		for (int i = 0; i < n; i++) {
			speaker = new KThread(speakerRun);
			speaker.setName("speaker" + i);

			listener = new KThread(listenRun);
			listener.setName("listener" + i);

			// I have no idea what this does
			// I just want some variance in speaker/listener first
			// I could use rng but it makes debugging harder
			if (i % 5 % 3 % 2 == 0) {
				speaker.fork();
				listener.fork();
			} else {
				listener.fork();
				speaker.fork();
			}
		}
		speaker.join();
		listener.join();

		for (int i = 0; i < n * 2; i++)
			KThread.yield();
	}

	/**
	 * Function to run inside Runnable object listenRun. Uses the function listen on
	 * static object myComm inside this class, allowing the threads inside the
	 * respective selfTests above to call the runnable variables below and test
	 * functionality for listen. Needs to run with debug flags enabled. See NACHOS
	 * README for info on how to run in debug mode.
	 */
	static void listenFunction() {
		// Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " is about to listen");

		myComm.listen();
		// Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " got value " + myComm.listen());

	} // listenFunction()

	/**
	 * Function to run inside Runnable object speakerRun. Uses the function listen
	 * on static object myComm inside this class, allowing the threads inside the
	 * respective selfTests above to call the runnable variables below and test
	 * functionality for speak. Needs to run with debug flags enabled. See NACHOS
	 * README for info on how to run in debug mode.
	 */
	static void speakFunction() {
		// Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " is about to speak");

		myComm.speak(myWordCount++);

		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " has spoken");
	} // speakFunction()

	/**
	 * Wraps listenFunction inside a Runnable object so threads can be generated for
	 * testing.
	 */
	private static Runnable listenRun = new Runnable() {
		public void run() {
			listenFunction();
		}
	}; // runnable listenRun

	/**
	 * Wraps speakFunction inside a Runnable object so threads can be generated for
	 * testing.
	 */
	private static Runnable speakerRun = new Runnable() {
		public void run() {
			speakFunction();
		}
	}; // Runnable speakerRun

	// Implement more test methods here ...

	// Invoke Communicator.selfTest() from ThreadedKernel.selfTest()

	public static void selfTest() {
		// selfTest1();
		// selfTest2();
		// selfTest3();
		selfTest4();

		// Invoke your other test methods here ...

	}

	// dbgThread = 't' variable needed for debug output
	private static final char dbgThread = 'c';
	// myComm is a shared object that tests Communicator functionality
	private static Communicator myComm = new Communicator();
	// myWordCount is used for selfTest5 when spawning listening/speaking threads
	private static int myWordCount = 0;

	private Lock lock = new Lock();

	private Condition canListen = new Condition(lock);
	private Condition canSpeak = new Condition(lock);

	private boolean words = false;

	private int word;
}
