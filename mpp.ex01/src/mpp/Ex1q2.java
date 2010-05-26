package mpp;

import java.util.concurrent.locks.ReentrantLock;

public class Ex1q2 {
	
	public static final int N = 10000;
	public static int sharedCounter = 0;
	public static ReentrantLock counterLock = new ReentrantLock();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// check input
		if (args.length != 1){
			System.err.println("must have a positive integer as input");
			System.exit(-1);
		}
		int n = Integer.valueOf(args[0]);
		if (n <= 0 || 1024 <= n){
			System.err.println("input must be a positive and at most 1024");
			System.exit(-1);
		}
		
		long startTime;
		long endTime;
		
		MyThread[] threads = new MyThread[n];
		System.out.println("Running with " + n + " threads:");
		
		// start timing
		startTime = System.nanoTime();

		int j;
		for (j=0; j<n; j++){
			threads[j] = new MyThread(j);
		}	
		for (j=0; j<n; j++){
			threads[j].start();
		}	
		try {
			for (j=0; j<n; j++)
				threads[j].join();
		}
		catch (InterruptedException ie) {};

		// end timing
		endTime = System.nanoTime();

		System.out.println("Done! shared counter value: " + sharedCounter + "\n");
		
		// print out results
		System.out.println("Result:\n" +
				"==============================================================================");
		System.out.println("n = " + n + "\t| " +
				"total running time = " + (endTime - startTime) + " nanosecs\t| " +
				"shared counter value = " + sharedCounter);
	}
	
	private static class MyThread extends Thread {
		final private int id;

	    public MyThread(int id){
	    	this.id = id;
	    }

	    public void run(){
	    	for (int myCounter = 0; myCounter < N; myCounter++){
	    		// lock
	    		counterLock.lock();
	    		
	    		// critical section
	    		int tmp = sharedCounter;
	        	tmp++;
	        	sharedCounter = tmp;
	        	
	        	// unlock
	        	counterLock.unlock();
	    	}
	    	
	    	System.out.println("thread " + this.id + " is done");
	    }
	}
}
