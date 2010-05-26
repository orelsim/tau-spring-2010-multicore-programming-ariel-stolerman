package mpp;

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * An implementation for the shared bathroom problem using locks and conditions.
 * Answer to exercise 96 (2) in the book
 */
public class SharedBathroomImpl2 {
	Lock lock = new ReentrantLock();
	AtomicReference<Gender> nextGenderToEnter = new AtomicReference<Gender>(null); 
	
	GenderParams males = new GenderParams(Gender.MALE);
	GenderParams females = new GenderParams(Gender.FEMALE);
	
	// class for holding each gender's parameters
	private class GenderParams{
		Gender gender;
		volatile int counter = 0;
		volatile boolean waiting = false;
		
		public GenderParams(Gender gender) { this.gender = gender; };
	}
	
	// Enumerator for gender
	private enum Gender{MALE, FEMALE}
	
	// generic methods for enter and leave operations
	
	/**
	 * apply enter bathroom for the given gender
	 */
	public synchronized void enter(GenderParams myGender){
		GenderParams otherGender = (myGender.gender == Gender.MALE) ? females : males;
		while (true){
			try{
				// if any of the opposite sex is inside, wait and mark my gender to be next to enter
				while (otherGender.counter > 0){
					nextGenderToEnter.set(myGender.gender);
					myGender.waiting = true;
					// using wait per gender so notification will release only one gender
					myGender.wait();
				}
				// if any of the opposite sex is waiting wait also,
				// unless this is the next gender to enter
				while (otherGender.waiting && nextGenderToEnter.get() != myGender.gender){
					myGender.waiting = true;
					myGender.wait();
				}
				// else update counter, mark other gender to enter next and enter
				myGender.counter++;
				nextGenderToEnter.set(otherGender.gender);
				break;
			} catch (InterruptedException ie){
				// try again
				continue;
			}
		}
	}
	
	/**
	 * apply leave bathroom for the given gender
	 */
	public synchronized void leave(GenderParams myGender){
		GenderParams otherGender = (myGender.gender == Gender.MALE) ? females : males;
		// update counter
		myGender.counter--;
		// alert opposite sex if needed
		if (myGender.counter == 0){
			// notify all threads from the opposite gender only
			otherGender.notifyAll();
			otherGender.waiting = false;
		}
	}
	
	// gender-specific methods
	public void enterMale(){ enter(males); }
	public void leaveMale(){ leave(males); }
	public void enterFemale(){ enter(males); }
	public void leaveFemale(){ leave(males); }
}
