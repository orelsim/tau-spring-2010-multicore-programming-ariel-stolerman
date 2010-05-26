package mpp;

import java.lang.Thread;

public class Ex2q1 {
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
		private volatile boolean[] flag = new boolean[2];
		private volatile int victim;
		
		public void lock() {
			int i = ((MyThread)Thread.currentThread()).getMyThreadId();
			int j = 1 - i;
			flag[i] = true; // I’m interested
			victim = i; // you go first
			while (flag[j] && victim == i) {} // wait
		}
		
		public void unlock() {
			int i = ((MyThread)Thread.currentThread()).getMyThreadId();
			flag[i] = false; // I’m not interested
		}
	}
}

