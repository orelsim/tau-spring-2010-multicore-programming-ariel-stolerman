package mpp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import mpp.Main.*;

/**
 * Priority Queue implemented in a linked list
 * Each operation is protected with one single TOLock
 */
public class PriorityQueue1 extends PriorityQueue{
	// time-out parameters
	private TimeUnit units;
	private long timeout;
	
	// lock and linked list head
	private TOLock lock;
	private Elem1 head = null;
	
	/**
	 * constructor
	 * @param units
	 * 				time units for timeout
	 * @param timeout
	 * 				time-out in given units for the TOLock
	 */
	public PriorityQueue1(TimeUnit units, long timeout){
		lock = new TOLock();
		
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
	public PriorityQueue1(int initialPriorites, TimeUnit units, long timeout){
		lock = new TOLock();
		Elem1 prevElem = head;
		
		for (int i=1; i<=initialPriorites; i++){
			Elem1 elem = new Elem1(i);
			if (head == null){
				head = elem;
				prevElem = elem;
			} else {
				prevElem.next = elem;
				prevElem = elem;
			}
		}
		
		this.timeout = timeout;
		this.units = units;
	}
	
	/**
	 * Inserts a new element by its priority
	 * @param val
	 * 			Value to be inserted to the priority queue
	 * @throws TimeoutException
	 * 			If the queue-lock had timed-out
	 */
	public void insert(int val) throws TimeoutException{
		// try to obtain lock
		try{
			boolean locked = lock.tryLock(timeout, units);
			if (locked){
				// lock attained, insert value by its priority
				Elem1 newElem = new Elem1(val);
				if (head == null){
					// queue is empty, insert as first element
					head = newElem;
				} else if (head.value >= newElem.value){
					// the new element is the minimum
					newElem.next = head;
					head = newElem;
				} else {
					// find the new element's priority and insert it to the queue
					Elem1 prev = head;
					Elem1 here = head;
					while (here != null && here.value < newElem.value){
						prev = here;
						here = here.next;
					}
					prev.next = newElem;
					newElem.next = here;
				}
			} else
				throw new TimeoutException();
		} catch (InterruptedException ie){
			int id = ((MyThread)Thread.currentThread()).getMyThreadID();
			System.out.println(">> Thread "+id+" interrupted during insert");
		} finally {
			// release lock anyway
			lock.unlock();
		}
	}
	
	/**
	 * removes and returns the first element
	 * if the queue is empty returns -1
	 * @return
	 * 			The minimum value in the priority queue
	 * @throws TimeoutException
	 * 			If the queue-lock had timed-out
	 */
	public int deleteMin() throws TimeoutException{
		int min = -1;
		boolean locked = false;
		try{
			locked = lock.tryLock(timeout, units);
			if (locked){
				// if the queue is empty
				if (head != null){
					// remove first element
					min = head.value;
					head = head.next;
				}
			} else 
				throw new TimeoutException();
		} catch (InterruptedException ie){
			int id = ((MyThread)Thread.currentThread()).getMyThreadID();
			System.out.println(">> Thread "+id+" interrupted during deleteMin or queue is empty");
		} finally {
			// release lock anyway
			lock.unlock();
		}
		return min;
	}
	
	public String toString(){
		String str = "";
		Elem1 curr = head;
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
	private static class Elem1{
		private Elem1 next = null;
		private int value;

		public Elem1(int value){
			this.value = value;
		}

		public String toString(){
			return this.value+"";
		}
	}
}