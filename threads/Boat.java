package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;
	static boolean not_done;
	static Lock lock;

	// Track whether the boat is on Oahu
	static boolean boat_is_on_oahu;
	
	// Track quantities of people in places
	static int num_child_boat;
	static int num_adult_oahu;
	static int num_child_oahu;

	// Track quantities of people not sleeping
	static int num_child_awake;
	static int num_adult_awake;

	// Queues for people sleeping in a given location
	static Condition oahu_adult;
	static Condition oahu_child;
	static Condition molokai_child;
	static Condition boat_child;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		// System.out.println("\n ***Testing Boats with only 2 children***");
		// begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with only 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adult***");
		begin(6, 2, b);

	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables
		not_done = true;
		lock = new Lock();

		// Boat starts on Oahu
		boat_is_on_oahu = true;

		// All people start on Oahu
		num_child_boat = 0;
		num_adult_oahu = adults;
		num_child_oahu = children;

		// All threads start awake
		num_adult_awake = num_adult_oahu;
		num_child_awake = num_child_oahu;

		// All threads should sleep on the class lock
		oahu_adult = new Condition(lock);
		oahu_child = new Condition(lock);
		molokai_child = new Condition(lock);
		boat_child = new Condition(lock);

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
		}

		// Spawn all child threads.
		for (int i = 0; i < children; i++) {
			new KThread(r_child).setName("Child " + Integer.toString(i + 1)).fork();
		}

		// Hold main thread until everyone is on Molokai
		while (not_done) {
			KThread.yield();
		}

	}

	static void AdultItinerary() {
		// adult threads can only operate with the lock atomically
		lock.acquire();

		// sleep until a child wakes us
		// make sure there is at least one child run the simulation
		num_adult_awake--;
		if (num_adult_awake + num_child_awake == 0) {
			// if this condition is true, we can start the simulation
			// we no longer need to track number of awake threads
			oahu_child.wake();
		}
		oahu_adult.sleep();

		// row adult self to Molokai and wake one child up so it can bring the
		// boat back to Oahu for another adult or last children
		bg.AdultRowToMolokai();
		num_adult_oahu--;
		boat_is_on_oahu = false;

		// wake a child to return the boat
		molokai_child.wake();

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
		if (num_child_awake > 1 || num_adult_awake > 0) {
			num_child_awake--;
			oahu_child.sleep();
		}

		// while there are still adults and children on Oahu
		while (not_done) {

			// if the boat is not on Oahu and we aren't done
			// then we need to return it
			while (!boat_is_on_oahu) {
				num_child_boat++;

				bg.ChildRowToOahu();
				boat_is_on_oahu = true;
				num_child_oahu++;

				num_child_boat--;
			} // end while (!boat_is_on_oahu)

			// if there is another child on Oahu
			// then we should go with them to Molokai
			// ignore if boat is on Molokai, since that means we are as well
			if (num_child_oahu >= 2 && boat_is_on_oahu) {

				// if we are the first on the boat, get a rower to take us
				if (num_child_boat == 0) {
					// wake the rower
					oahu_child.wake();

					num_child_boat++;

					// sleep until the rower wakes us
					boat_child.sleep();

					// rower will wake us on the island
					// we can go back to sleep
					boat_child.wake();
					molokai_child.sleep();

				}
				// if we are second on the boat, we are the rower
				else if (num_child_boat == 1) {
					num_child_boat++;

					bg.ChildRowToMolokai();
					bg.ChildRideToMolokai();
					boat_is_on_oahu = false;
					num_child_oahu -= 2;

					// throw the passenger out of the boat
					// that should wake them up
					num_child_boat--;
					boat_child.wake();
					boat_child.sleep();

					// if there is no one on Oahu, we're done
					if (num_child_oahu + num_adult_oahu == 0) {
						not_done = false;
						return;
					}
					// otherwise, we need to return the boat to Oahu
					else {
						bg.ChildRowToOahu();
						boat_is_on_oahu = true;
						num_child_oahu++;

						num_child_boat--;
					}
				}
			} // end while (num_child_oahu >= 2 && boat_is_on_oahu)

			// if we're on Oahu and there is an adult to wake
			// then we should wake one, then go to sleep
			// this condition will never be reached if there is another child
			// ignore if boat is on Molokai, since that means we are as well
			else if (num_adult_oahu > 0 && boat_is_on_oahu) {
				oahu_adult.wake();
				oahu_child.sleep();
			} // end while (num_adult_oahu > 0 && boat_is_on_oahu)

			// if we're the only one on Oahu
			// then we should row to Molokai, and we're done
			// this condition will never be reached if anyone else is on Oahu
			// ignore if boat is on Molokai, since that means we are as well
			/*while (num_child_oahu == 1 && boat_is_on_oahu) {
				num_child_boat++;

				bg.ChildRowToMolokai();
				boat_is_on_oahu = true;
				num_child_oahu--;

				num_child_boat--;

				not_done = false;
				return;
			}*/

		} // end while (not_done)

		lock.release();
	} // end ChildItinerary()

}
