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

		System.out.println("\n Testing Boats with 59 children, 58 adults");
		begin(59, 58, b);

		System.out.println("\n Testing Boats with 100 children, 100 adults");
		begin(100, 100, b);


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
		// adult threads can only operate with the lock atomically
		lock.acquire();

		// sleep until a child wakes us
		// make sure there is at least one child run the simulation
		awake_adults--;
		if (awake_adults + awake_children == 0) {
			// if this condition is true, we can start the simulation
			// we no longer need to track number of awake threads
			oahuChild.wake();
		}
		oahuAdult.sleep();

		// row adult self to Molokai and wake one child up so it can bring the
		// boat back to Oahu for another adult or last children
		bg.AdultRowToMolokai();
		num_adult_Oahu--;
		boat_is_on_oahu = false;

		// wake a child to return the boat
		molokaiChild.wake();

		// once an adult is on Molokai, we no longer need to track them
		lock.release();
		KThread.finish();
	}

	static void ChildItinerary() {
		// child threads can only operate with the lock atomically
		lock.acquire();

		// make sure everyone else is asleep before we start
		// only runs once per thread, at start of simulation
		// after this, we no longer need to track number of awake threads
		if (awake_children > 1 || awake_adults > 0) {
			awake_children--;
			oahuChild.sleep();
		}

		// while there are still adults and children on Oahu
		while (not_done) {

			// if the boat is not on Oahu and we aren't done
			// then we need to return it
			while (!boat_is_on_oahu) {
				children_on_boat++;

				bg.ChildRowToOahu();
				boat_is_on_oahu = true;
				num_child_Oahu++;

				children_on_boat--;
			} // end while (!boat_is_on_oahu)

			// if there is another child on Oahu
			// then we should go with them to Molokai
			// ignore if boat is on Molokai, since that means we are as well
			while (num_child_Oahu >= 2 && boat_is_on_oahu) {

				// if we are the first on the boat, get a rower to take us
				if (children_on_boat == 0) {
					// wake the rower
					oahuChild.wake();

					children_on_boat++;

					// sleep until the rower wakes us
					boatChild.sleep();

					// rower will wake us on the island
					// we can go back to sleep
					molokaiChild.sleep();

				}
				// if we are second on the boat, we are the rower
				else if (children_on_boat == 1) {
					children_on_boat++;

					bg.ChildRowToMolokai();
					bg.ChildRideToMolokai();
					boat_is_on_oahu = false;
					num_child_Oahu -= 2;

					// throw the passenger out of the boat
					// that should wake them up
					children_on_boat--;
					boatChild.wake();

					// if there is no one on Oahu, we're done
					if (num_child_Oahu + num_adult_Oahu == 0) {
						not_done = false;
						return;
					}
					// otherwise, we need to return the boat to Oahu
					else {
						bg.ChildRowToOahu();
						boat_is_on_oahu = true;
						num_child_Oahu++;

						children_on_boat--;
					}
				}
			} // end while (num_child_Oahu >= 2 && boat_is_on_oahu)

			// if we're on Oahu and there is an adult to wake
			// then we should wake one, then go to sleep
			// this condition will never be reached if there is another child
			// ignore if boat is on Molokai, since that means we are as well
			while (num_adult_Oahu > 0 && boat_is_on_oahu) {
				oahuAdult.wake();
				oahuChild.sleep();
			} // end while (num_adult_Oahu > 0 && boat_is_on_oahu)

			// if we're the only one on Oahu
			// then we should row to Molokai, and we're done
			// this condition will never be reached if anyone else is on Oahu
			// ignore if boat is on Molokai, since that means we are as well
			while (num_child_Oahu == 1 && boat_is_on_oahu) {
				children_on_boat++;

				bg.ChildRowToMolokai();
				boat_is_on_oahu = true;
				num_child_Oahu--;

				children_on_boat--;

				not_done = false;
				return;
			}

		} // end while (not_done)
		lock.release();
	} // end ChildItinerary()

}
