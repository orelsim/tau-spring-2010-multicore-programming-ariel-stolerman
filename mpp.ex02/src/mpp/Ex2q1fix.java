package mpp;

import java.lang.Thread;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Ex2q1fix {
	public static final int N = 100000;
	public static final int waitBound = 30*1000;
	public static int sharedCounter = 0;
	public static PetersonLock peterson = new PetersonLock();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int n = 2, i;
		long startTime;
		long endTime;
		
		// start timing
		startTime = System.nanoTime();
		
		// initialize threads
		MyThread[] threads = new MyThread[n];
		for (i=0; i<n; i++)
			threads[i] = new MyThread(i);
		
		// start running
		for (i=0; i<n; i++)
			threads[i].start();
		try {
			for (i=0; i<n; i++)
				threads[i].join(waitBound);
		}
		catch (InterruptedException ie) {};
		
		// end timing
		endTime = System.nanoTime();
		
		// print results
		System.out.println("Done running " + n + " threads. Shared counter value is: "
				+ sharedCounter);
		System.out.println("Total running time: "+(endTime-startTime));
	}
	
	private static class MyThread extends Thread {
		final private int id;

	    public MyThread(int id){
	    	this.id = id;
	    }
	    
	    public int getMyThreadId(){
	    	return this.id;
	    }

	    public void run(){
	    	for (int i=0; i<N; i++){
	    		peterson.lock();
	    		sharedCounter++;
	    		peterson.unlock();
	    	}
	    	System.out.println("Thread " + id + " is done");
	    }
	}
	
	// Peterson lock implementation
	private static class PetersonLock{
		// thread-local index, 0 or 1
		private AtomicReferenceArray<AtomicBoolean> flag = initAtomicRefArray();
		private volatile int victim;
		
		/**
		 * @return	an atomic array (of size 2) of atomic booleans
		 */
		private static AtomicReferenceArray<AtomicBoolean> initAtomicRefArray(){
			AtomicReferenceArray<AtomicBoolean> flag = new AtomicReferenceArray<AtomicBoolean>(2);
			flag.set(0, new AtomicBoolean());
			flag.set(1, new AtomicBoolean());
			return flag;
		}
		
		public void lock() {
			int i = ((MyThread)Thread.currentThread()).getMyThreadId();
			int j = 1 - i;
			flag.get(i).set(true); // I’m interested
			victim = i; // you go first
			while (flag.get(j).get() && victim == i) {} // wait
		}
		
		public void unlock() {
			int i = ((MyThread)Thread.currentThread()).getMyThreadId();
			flag.get(i).set(false); // I’m not interested
		}
	}
}

