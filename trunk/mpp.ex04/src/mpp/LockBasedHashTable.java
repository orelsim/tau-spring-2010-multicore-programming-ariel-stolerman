package mpp;

import java.util.*;
import java.util.concurrent.atomic.*;
//import java.util.concurrent.locks.*;

public class LockBasedHashTable {
	// for debugging
	public static boolean debug = true;
	
	// load factor
	public static int L = 10;
	public static final int initCapacity = 1;
	public static final int defaultNumOfLocks = 2; 
	
	// members
	//Lock[] locks = null;
	AtomicBoolean[] locks = null;
	private int numOfLocks;
	private Vector<Bucket> table;
	private AtomicInteger tableSize;
	private AtomicInteger totalElements;
	private AtomicBoolean isRehashingInProgress;
	
	/**
	 * default constructor
	 * @param numOfLocks	initial number of locks
	 * if the initial number of locks is not a power of 2, sets default of defaultNumOfLocks locks
	 */
	public LockBasedHashTable(int numOfLocks){
		// initialize locks
		double log2locks = Math.log(numOfLocks)/Math.log(2);
		if (log2locks - Math.floor(log2locks) != 0.0){
			System.out.println("Number of locks must be a power of 2, setting default: 2 locks");
			numOfLocks = defaultNumOfLocks;
		}
		//locks = new ReentrantLock[numOfLocks];
		locks = new AtomicBoolean[numOfLocks];
		for (int i=0; i<numOfLocks; i++){
			//locks[i] = new ReentrantLock();
			locks[i] = new AtomicBoolean(false);
		}
		this.numOfLocks = numOfLocks;
		
		// initialize vector of buckets
		table = new Vector<Bucket>(0);
		for (int i=0; i<initCapacity; i++){
			table.add(i, new Bucket());
		}
		tableSize = new AtomicInteger(initCapacity);
		totalElements = new AtomicInteger(0);
		
		// initialize compare-and-set register indicating rehashing is in progress to false
		isRehashingInProgress = new AtomicBoolean(false);
	}
	
	/**
	 * puts the key-value pair in the correct bucket in the hash-table
	 */
	public Object put(Object key, Object value){
		// lock and get the correct bucket
		int[] indices = lockAndGetIndices(key);		// position 0: bucket index; position 1: lock index
		Bucket bucket = table.get(indices[0]);
		
		// get the old value associated with the key, remove it if found
		Object oldValue = null;
		Elem elem = bucket.head;
		while (elem != null){
			if (elem.key.hashCode() == key.hashCode()){
				// found match, stop searching and remove the element from the bucket
				oldValue = elem.value;
				bucket.remove(elem);
				totalElements.getAndDecrement();
				break;
			} else elem = elem.succ;
		}
		
		// put the key-value pair in the correct bucket (as the new head)
		Elem newElem = new Elem(key,value);
		bucket.add(newElem);
		totalElements.getAndIncrement();
		
		// check and rehash table if needed
		if (totalElements.get()/tableSize.get() > L){
			//locks[indices[1]].unlock();
			locks[indices[1]].set(false);
			rehashTable();			// a check that rehashing is not done by some other thread is
		} else {					// done at the beginning of rehashTable()
			//locks[indices[1]].unlock();
			locks[indices[1]].set(false);
		}
		
		// return the old value mapped to key or null if no such existed
		return oldValue;
	}

	/**
	 * returns the value mapped to the key in the hash-table or null if doesn't exist
	 */
	public Object get(Object key){
		// lock and get the correct bucket
		int[] indices = lockAndGetIndices(key);		// position 0: bucket index; position 1: lock index
		Bucket bucket = table.get(indices[0]);
		
		// get the value associated with the key or null if doesn't exist
		Object value = bucket.get(key);
		
		// unlock the bucket and return the result
		//locks[indices[1]].unlock();
		locks[indices[1]].set(false);
		return value;
	}
	
	/* ***************
	 * private methods
	 *****************/
	
	/**
	 * locks the correct bucket for the given key and returns its index and the lock index
	 */
	private int[] lockAndGetIndices(Object key){
		int bucketIndex;
		int lockIndex;
		
		// don't try to attain lock while rehashing is in progress
		while (isRehashingInProgress.get()) {}
		
		// try to attain the correct bucket's lock until succeeded
		while (true){
			bucketIndex = getBucketIndex(key, tableSize.get());	// maybe the table is being rehashed right now...
			lockIndex = bucketIndex % numOfLocks;
			// attain lock
			//locks[lockIndex].lock();
			while (!locks[lockIndex].compareAndSet(false, true)) {}
			// validate that the correct bucket was locked
			// and that rehashing wasn't done in the meanwhile or try again
			if (getBucketIndex(key,tableSize.get()) == bucketIndex && !isRehashingInProgress.get())
				break;
			else {
				//locks[lockIndex].unlock();
				locks[lockIndex].set(false);
				continue;
			}
		}
		return new int[]{bucketIndex,lockIndex};
	}
	
	/**
	 * return the hashed index for the given key to point to the correct bucket
	 */
	private int getBucketIndex(Object key, int currTableSize){
		int index = key.hashCode() % currTableSize;
		if (index < 0) index += currTableSize;
		return index;
	}
	
	/**
	 * rehashing the table
	 */
	private void rehashTable(){
		// try to mark this thread as the one rehashing the table, and leave if failed
		// (some other thread is already rehashing)
		if (!isRehashingInProgress.compareAndSet(false, true)) return;
		if (debug) {
			int threadId = ((Main.MyLockBasedHashTableThread)Thread.currentThread()).getMyThreadId();
			System.out.print(">>> rehash began by thread #"+threadId+" >>>");
		}
		
		// attain all locks
		//for(Lock lock: locks){
		//	lock.lock();
		//}
		for(int i=0; i<numOfLocks; i++){
			while(!locks[i].compareAndSet(false, true)){}
		}
		
		
		// create a new table
		int newCapacity = 2*table.size();
		Vector<Bucket> newTable = new Vector<Bucket>(0);
		for (int i=0; i<newCapacity; i++){
			newTable.add(i, new Bucket());
		}
		
		// reenter all values
		for (int i=table.size()-1; i>=0; i--){
			Bucket bucket = table.get(i);
			Elem elem = bucket.head;
			while (elem != null){
				int newBucketIndex = getBucketIndex(elem.key,newTable.size());
				newTable.get(newBucketIndex).add(elem);
				elem = elem.succ;
			}
			table.remove(i);
		}
		// update table pointer and size
		table = newTable;
		tableSize.set(newTable.size());
		
		// release all locks and mark that rehashing no longer in progress
		//for(Lock lock: locks){
		//	lock.unlock();
		//}
		for(int i=0; i<numOfLocks; i++){
			locks[i].set(false);
		}
		
		isRehashingInProgress.set(false);
		
		if (debug) System.out.println(" done: new table size: "+table.size());
	}

	/**
	 * bucket class
	 */
	private static class Bucket{
		// members
		protected Elem head = null;
		protected int size = 0;
		
		/**
		 * default constructor
		 */
		public Bucket(){}
		
		
		// getters
		
		/**
		 * returns the value mapped to the given key
		 */
		public Object get(Object key){
			Object value = null;
			Elem elem = head;
			while (elem != null){
				if (key.hashCode() == elem.key.hashCode()){
					value = elem.value;
					break;
				} else elem = elem.succ;
			}
			return value;
		}
		
		// setters
		
		/**
		 * adds the given element as the new head of the bucket
		 */
		public void add(Elem elem){
			elem.pred = null;
			elem.succ = head;
			head = elem;
			size++;
		}
		
		/**
		 * removes the given element from the bucket
		 * assumption: the element is contained in the bucket (not checked)
		 */
		public void remove(Elem elem){
			if (elem == null) return;
			Elem pred = elem.pred;
			Elem succ = elem.succ;
			if (pred != null) pred.succ = succ;
			else head = succ;					// succ is the new head of the bucket
			size--;
		}
	}
	
	/**
	 * bucket's element class
	 */
	private static class Elem{
		protected Object key;
		protected Object value;
		protected Elem pred = null;
		protected Elem succ = null;
		
		/**
		 * default constructor for an element in a bucket, containing the key and the value
		 */
		public Elem(Object key, Object value){
			this.key = key;
			this.value = value;
		}
	}
}
