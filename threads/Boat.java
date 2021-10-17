package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;
	static boolean not_done;
	static boolean boat_is_on_oahu;
	static Lock lock;
	static int children_on_boat;

	// your code here

	static int num_adult_Oahu;
	static int num_child_Oahu;

	static int awake_children;
	static int awake_adults;

	static Condition oahuAdult;
	static Condition oahuChild;
	static Condition molokaiChild;
	static Condition boatChild;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		// System.out.println("\n ***Testing Boats with only 2 children***");
		// begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with only 2 children, 1 adult***");
		// begin(1, 2, b);

		System.out.println("\n ***Testing Boats with 3 children, 3 adult***");
		begin(1, 3, b);

	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		not_done = true;
		boat_is_on_oahu = true;
		lock = new Lock();
		children_on_boat = 0;

		// your code here

		oahuAdult = new Condition(lock);
		oahuChild = new Condition(lock);
		molokaiChild = new Condition(lock);
		boatChild = new Condition(lock);

		num_adult_Oahu = adults;
		awake_adults = num_adult_Oahu;

		num_child_Oahu = children;
		awake_children = num_child_Oahu;

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		// Define runnable object for child thread.
		Runnable r_child = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		}; // r_child Runnable()

		// Define runnable object for adult thread.
		Runnable r_adult = new Runnable() {
			public void run() {
				AdultItinerary();
			}
		}; // r_adult Runnable()

		// Spawn all adult threads.
		for (int i = 0; i < adults; i++) {
			new KThread(r_adult).setName("Adult " + Integer.toString(i + 1)).fork();
		} // after this for loop, all adult threads are spawned and sleeping

		// Spawn all child threads.
		for (int i = 0; i < children; i++) {
			new KThread(r_child).setName("Child " + Integer.toString(i + 1)).fork();
		} // after this for loop, all child threads are spawned and start running

		// hold main thread while solutions calls are made to the BoatGrader
		while (not_done)
			KThread.yield();
		// while loop ends when last children and all adults are on Molokai

	}

	static void AdultItinerary() {

		/*
		 * This is where you should put your solutions. Make calls to the BoatGrader to
		 * show that it is synchronized. For example: bg.AdultRowToMolokai(); indicates
		 * that an adult has rowed the boat across to Molokai
		 */

		// adult threads can only operate with the lock atomically
		lock.acquire();

		oahuAdult.sleep();

		// while there are still adults not asleep on Molokai
		while (not_done) {

			// your code here

			// TODO: see if all 'if's are okay
			while (num_child_Oahu >= 2 || !boat_is_on_oahu) {
				oahuAdult.sleep();
				if (boat_is_on_oahu) {
					oahuChild.wake();
				}
			} // after while, boat is on Oahu and children do not need it.

			// row adult self to Molokai and wake one child up so it can bring the
			// boat back to Oahu for another adult or last children
			bg.AdultRowToMolokai();

			// your code here

			num_adult_Oahu--;

			boat_is_on_oahu = false;

			molokaiChild.wake();
			oahuChild.sleep();
			lock.release();

			KThread.finish();

			// your code here

		} // while not done and adult still need to get to Molokai
	}

	static void ChildItinerary() {
		// child threads can only operate with the lock atomically
		lock.acquire();

		if (awake_children > 1) {
			awake_children--;
			oahuChild.sleep();
		}

		// while there are still adults and children not on Molokai
		while (not_done) {

			// if the boat is not on Oahu
			// this child is woken up on Molokai by an adult to ferry boat
			// for other adults on Oahu or last children on Oahu
			while (!boat_is_on_oahu) {

				// your code here
				children_on_boat++;

				bg.ChildRowToOahu();

				num_child_Oahu++;
				boat_is_on_oahu = true;

				children_on_boat--;

			}

			while (num_child_Oahu >= 2 && boat_is_on_oahu) {
				// if this child will be the first into the boat, it will be a
				// passenger and wait for a rower
				if (children_on_boat == 0) {
					// we go get another child to row the boat
					oahuChild.wake();

					// we get in the boat
					children_on_boat++;

					// sleep until rower wakes us
					boatChild.sleep();

					// on the island, go to sleep
					molokaiChild.sleep();

				} else if (children_on_boat == 1) {
					// two children always bring the boat back from Oahu and check
					// if they are done before returning to Oahu for an adult
					children_on_boat++;
					bg.ChildRowToMolokai();
					bg.ChildRideToMolokai();
					boat_is_on_oahu = false;

					num_child_Oahu -= 2;

					// throw the passenger out of the boat
					children_on_boat--;

					// that should wake them up
					boatChild.wake();

					if (num_child_Oahu + num_adult_Oahu == 0) {
						// set terminal bool to false to end all loops and return
						not_done = false;
						return;
					} // boat terminates after this if statement is executed
					// else we are not done so we need to send one back to Oahu
					else {
						bg.ChildRowToOahu();

						num_child_Oahu++;

						children_on_boat = 0;
						boat_is_on_oahu = true;

						// your code here

					}
				}
			} // while (num_child_Oahu >= 2)
			
			while (num_adult_Oahu > 0  && boat_is_on_oahu) {
				oahuAdult.wake();
				oahuChild.sleep();
			}

			while (num_child_Oahu == 1  && boat_is_on_oahu) {
				children_on_boat++;

				bg.ChildRowToMolokai();

				num_child_Oahu--;
				children_on_boat--;

				not_done = false;
				return;
			}

		} // while (not_done)

		lock.release();
	} // ChildItinerary()

}
