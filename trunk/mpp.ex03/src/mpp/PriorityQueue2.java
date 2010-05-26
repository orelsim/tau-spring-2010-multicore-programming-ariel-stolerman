package mpp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import mpp.Main.*;

public class PriorityQueue2 extends PriorityQueue{
	private static int EMPTY = -1;
	
	// time-out parameters
	private TimeUnit units;
	private long timeout;
	
	// lock and linked list
	// making the head volatile saves many "empty" locks, that is locks that immediately are unlocked,
	// and so although the lock's AtomicReference synchronizes the cache, a volatile is added.
	private volatile Elem2 head = new Elem2(EMPTY); // "queue is empty"
	
	/**
	 * constructor
	 * @param units
	 * 				time units for timeout
	 * @param timeout
	 * 				time-out in given units for the TOLock
	 */
	public PriorityQueue2(TimeUnit units, long timeout){
		this.timeout = timeout;
		this.units = units;
	}
	
	/**
	 * constructor with initial queue priorities
	 * @param initialPriorites
	 * 				initial priorities bound to be inserted into the queue starting from 1
	 * @param units
	 * 				time units for timeout
	 * @param timeout
	 * 				time-out in given units for the TOLock
	 */
	public PriorityQueue2(int initialPriorites, TimeUnit units, long timeout){
		Elem2 prevElem = head;
		
		for (int i=1; i<=initialPriorites; i++){
			if (head.value == EMPTY){
				head.value = i;
			} else {
				Elem2 elem = new Elem2(i); 
				prevElem.next = elem;
				prevElem = prevElem.next;
			}
		}
		
		this.timeout = timeout;
		this.units = units;
	}
	
	/**
	 * Inserts a new element in its place
	 * @param val
	 * 				Value to be inserted to the priority queue
	 */
	public void insert(int val) throws TimeoutException{
		// elements for iterating over the queue
		Elem2 newElem=null, prevElem=null, nextElem=null;
		// indicators for my locks
		boolean headLocked=false, imLocked=false, prevLocked=false, nextLocked=false;
		
		try{
			// loop until you succeed
			while (true){
				Elem2 headTry = head;
				
				// case #1: the queue is empty
				if (headTry.value == EMPTY){
					// this is the first element
					headLocked = headTry.lock.tryLock(timeout, units);
					if (headLocked){
						if (headTry != head){
							// try again
							headTry.lock.unlock();
							headLocked = false;
							continue;
						}
						// this is indeed head as before
						if (head.value == EMPTY){
							// this is still the first element
							head.value = val;
							break; // stop looping
						} else {
							// try again
							head.lock.unlock();
							continue;
						}
					} else throw new TimeoutException();
				}
				
				// case #2: this is the new minimum
				else if (headTry.value >= val){
					headLocked = headTry.lock.tryLock(timeout, units);
					if (headLocked){
						if (headTry != head){
							// try again
							headTry.lock.unlock();
							headLocked = false;
							continue;
						}
						// this is indeed head as before
						if (!head.isDeleted && head.value >= val){
							// this is still the new minimum
							newElem = new Elem2(val);
							imLocked = newElem.lock.tryLock(timeout, units);
							newElem.next = head;
							head = newElem;
							head.next.lock.unlock();
							break; // stop looping
						} else {
							// try again
							head.lock.unlock();
							continue;
						}
					} else throw new TimeoutException();
				}
				
				// case #3: this element should be inserted somewhere along the queue
				else {
					// create new element to be inserted
					newElem = new Elem2(val);
					imLocked = newElem.lock.tryLock(timeout, units); // should always succeed
					
					// get the position to insert the new element into
					prevElem = head;
					nextElem = prevElem.next;
					while (nextElem != null && nextElem.value < val){
						prevElem = nextElem;
						nextElem = prevElem.next;
					}
										
					// attain locks
					prevLocked = prevElem.lock.tryLock(timeout, units);
					if (!prevLocked) throw new TimeoutException();
					if (nextElem != null){
						nextLocked = nextElem.lock.tryLock(timeout, units);
						if (!nextLocked){
							prevElem.lock.unlock();
							throw new TimeoutException();
						}
					}
					
					// check that the conditions still apply
					if (validate(prevElem,nextElem)){
						// insert new element in the middle
						newElem.next = nextElem;
						prevElem.next = newElem;
						break; // stop looping
					} else {
						// try again
						prevElem.lock.unlock();
						if (nextElem != null) nextElem.lock.unlock();
						continue;
					}
				}
			}
		} catch (InterruptedException ie){
			int id = ((MyThread)Thread.currentThread()).getMyThreadID();
			System.out.println(">> Thread "+id+" interrupted during insert");
		} finally {
			// anyway free all elements that I locked (also on success)
			head.lock.unlock();		// free head that I locked
			if (imLocked) newElem.lock.unlock();	// free the new element I created
			if (prevLocked) prevElem.lock.unlock();	// free prev that I locked
			if (nextLocked) nextElem.lock.unlock();	// free next that I locked
		}
	}
	
	/**
	 * validation before actual insertion that the status before lock was not changed
	 */
	private boolean validate(Elem2 prevElem, Elem2 nextElem){
		if (nextElem == null) return (!prevElem.isDeleted && prevElem.next == null);
		else return (!prevElem.isDeleted && prevElem.next == nextElem && !nextElem.isDeleted);
	}
	
	/**
	 * removes and returns the first element
	 * if the queue is empty returns -1
	 * @return
	 * 			The minimum value in the priority queue
	 * @throws TimeoutException
	 * 			If the head-element lock had timed-out
	 */
	public int deleteMin() throws TimeoutException{
		int min = -1;
		boolean locked = false;
		Elem2 headTry;
		try{
			while (true){
				// attain head's lock
				headTry = head;
				locked = headTry.lock.tryLock(timeout, units);
				if (locked){
					// check that indeed head was locked
					if (headTry != head){
						// try again
						headTry.lock.unlock();
						locked = false;
						continue;
					}
					
					if (head.value == EMPTY) return min; // queue is empty (at the time I see head.value)
					if (head.isDeleted){
						// head is already deleted, try again
						head.lock.unlock();
						locked = false;
						System.out.println("Thread "+((MyThread)Thread.currentThread()).getMyThreadID()+": head is deleted");
						System.out.println(this.toString());
						continue;
					} else if (head.next == null){
						// queue has only one element
						min = head.value;
						// mark "queue is empty" again
						head.value = EMPTY;
						return min;
					} else {
						// mark current head as deleted before actual deleting
						head.isDeleted = true;
						min = head.value;
						Elem2 old = head;
						head = head.next;
						old.lock.unlock();
						return min;
					}
				} else throw new TimeoutException();
			}
		} catch (InterruptedException ie){
			int id = ((MyThread)Thread.currentThread()).getMyThreadID();
			System.out.println(">> Thread "+id+" interrupted during deleteMin");
		} finally {
			// anyway free the head that I locked
			head.lock.unlock();
		}
		return min;
	}
	
	public String toString(){
		String str = "";
		Elem2 curr = head;
		while (curr != null){
			str += curr.value+", ";
			curr = curr.next;
		}
		str = "["+((curr==head)?"":str.substring(0, str.length()-2))+"]";
		return str;
	}
	
	/**
	 * class for PriorityQueue1 elements
	 */
	private static class Elem2{
		private Elem2 next = null;
		private int value;
		private boolean isDeleted = false;
		private TOLock lock = new TOLock();

		public Elem2(int value){
			this.value = value;
		}

		public String toString(){
			return super.toString()+(isDeleted?",DELETED":"");
		}
	}
}