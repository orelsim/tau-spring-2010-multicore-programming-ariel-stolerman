package mpp;

import java.io.ObjectInputStream.GetField;
import java.util.*;

public class Main {
	
	// variables for testing LockBasedHashTable
	private static int numOfHashTableOperations = 1000000;
	private static int[] numOfLocksArray = {1,2,8,32,256,1024,4096};
	private static int[] loadFactorArray = {10,20,50,100};
	private static LockBasedHashTable table;
	private static int numOfHashTableThreads = 8;
	
	// variables for testing LockFreeStack
	private static int numOfStackThreads = 4;
	private static int[] possibleValues = {0,1,2,3,4,5,6,7,8,9,10};
	private static int acqVal = possibleValues[possibleValues.length/2];
	private static LockFreeStack stack;
	private static int numOfStackOperations = 10;
	
	// variables for testing TwoStackQueue
	private static int numOfQueueThreads = 4;
	private static int queueValuesBound = 100;
	private static TwoStackQueue queue;
	private static int numOfQueueOperations = 20;

	/**
	 * main for testing
	 */
	public static void main(String[] args) {
		
		// timing
		long startTime;
		long endTime;
		
		/* **************************
		 * Testing LockBasedHashTable
		 ****************************/ 
		MyLockBasedHashTableThread[] hashTableThreads = new MyLockBasedHashTableThread[numOfHashTableThreads];
		HashMap<String,String> runningTimes = new HashMap<String,String>(); 
		
		for (int loadFactor: loadFactorArray){
			// set current load factor
			LockBasedHashTable.L = loadFactor;
			
			// try various number of locks
			for (int numOfLocks: numOfLocksArray){
				// create LockBasedHashTable
				table = new LockBasedHashTable(numOfLocks);
				if (LockBasedHashTable.debug)
					System.out.println(">>> test started for "+loadFactor+" load factor and "+numOfLocks+" locks");
				
				// start timing
				startTime = System.nanoTime();
				
				// run all threads
				for(int i=0; i<numOfHashTableThreads; i++) hashTableThreads[i] = new MyLockBasedHashTableThread(i);
				for(int i=0; i<numOfHashTableThreads; i++) hashTableThreads[i].start();
				try{
					for(int i=0; i<numOfHashTableThreads; i++) hashTableThreads[i].join();
				} catch (InterruptedException ie) { System.err.println("Interrupted!"); };
				
				// end timing
				endTime = System.nanoTime();
				
				// print results
				String key = loadFactor+""+numOfLocks; 
				runningTimes.put(key, (endTime-startTime)+"");
				if (LockBasedHashTable.debug)
					System.out.println(">>> done for "+loadFactor+" load factor and "+numOfLocks+" locks "+
							"in "+(endTime-startTime)+" nanosecs");
			}
		}
		
		// print results
		String header = "load factor \\ #locks |";
		for (int numOfLocks: numOfLocksArray){
			header += pad(numOfLocks+"",14,1)+"|";
		}
		int sepSize = header.length();
		header += "\n";
		for (int i=0; i<sepSize; i++){
			header += "=";
		}
		String smallSep = "";
		for (int i=0; i<sepSize; i++){
			smallSep += "-";
		}
		System.out.println(header);
		for (int loadFactor:loadFactorArray){
			String row = pad(loadFactor+"",21,0)+"|";
			for (int numOfLocks: numOfLocksArray){
				String runningTime = runningTimes.get(loadFactor+""+numOfLocks);
				row += pad(runningTime+"",15,1)+"|";
			}
			row += "\n"+smallSep;
			System.out.println(row);
		}
		
		/* *********************
		 * Testing LockFreeStack
		 ***********************/ /*
		MyLockFreeStackThread[] stackThreads = new MyLockFreeStackThread[numOfStackThreads];
		
		// create LockFreeStack
		stack = new LockFreeStack();
		if (LockFreeStack.debug) System.out.println(">>> LockFreeStack test started");
		
		// start timing
		startTime = System.nanoTime();
		
		// run all threads
		for(int i=0; i<numOfStackThreads; i++) stackThreads[i] = new MyLockFreeStackThread(i);
		for(int i=0; i<numOfStackThreads; i++) stackThreads[i].start();
		try{
			for(int i=0; i<numOfStackThreads; i++) stackThreads[i].join();
		} catch (InterruptedException ie) { System.err.println("Interrupted!"); };
		
		// end timing
		endTime = System.nanoTime();
		
		/* *********************
		 * Testing TwoStackQueue
		 ***********************/ /*
		MyTwoStackQueueThread[] queueThreads = new MyTwoStackQueueThread[numOfQueueThreads];
		
		// create TwoStackQueue
		queue = new TwoStackQueue();
		if (TwoStackQueue.debug) System.out.println(">>> TwoStackQueue test started");
		
		// start timing
		startTime = System.nanoTime();
		
		// run all threads
		for(int i=0; i<numOfQueueThreads; i++) queueThreads[i] = new MyTwoStackQueueThread(i);
		for(int i=0; i<numOfQueueThreads; i++) queueThreads[i].start();
		try{
			for(int i=0; i<numOfQueueThreads; i++) queueThreads[i].join();
		} catch (InterruptedException ie) { System.err.println("Interrupted!"); };
		
		// end timing
		endTime = System.nanoTime();
		
		/* **************
		 * done testing
		 ****************/
	}
	
	// for printing results
	public static String pad(String initial, int size, int before){
		String str = "";
		for (int i=0; i<before; i++) str += " ";
		str += initial;
		int max = size - str.length();
		for (int i=0; i<max; i++) str += " ";
		return str;
	}

	/**
	 * MyThread class for testing
	 */
    public static abstract class MyThread extends Thread{
    	final private int id;
    	protected int failures = 0;

    	public MyThread(int id){
    		this.id = id;
    	}

    	public int getMyThreadId(){
    		return id;
    	}

    	public int getNumOfFailures(){
    		return failures;
    	}

    	abstract public void run();
    }
    
	/**
     * Thread class for testing LockBasedHashTable
     */
    public static class MyLockBasedHashTableThread extends MyThread{
    	public MyLockBasedHashTableThread(int id){
    		super(id);
    	}
    	
    	public void run(){
    		Random keyRand = new Random();
    		Random valueRand = new Random();
    		for(int i=0; i<numOfHashTableOperations; i++){
    			table.put(keyRand.nextInt(), valueRand.nextInt());
    		}
    	}
    }
    
    /**
     * Thread class for testing LockFreeStack
     */
    public static class MyLockFreeStackThread extends MyThread{
    	public MyLockFreeStackThread(int id){
    		super(id);
    	}
    	
    	public void run(){
    		Random rand = new Random();
    		for (int i=0; i<numOfStackOperations; i++){
    			// randomly pick a value
    			int index = rand.nextInt(possibleValues.length);
    			int value = possibleValues[index];
    			while (true){
    				// push, wait and pop
    				try{
    					// if the value is in the middle of possibleValues, acquire, push, sleep, pop and release
    					if (value == acqVal) stack.acquire();
    					stack.push(value);
    					Thread.sleep(10);
    					stack.pop();
    					if (value == acqVal) stack.release();
    					break;
    				} catch (RuntimeException re){
    					if (LockFreeStack.debug) System.out.println(">>>"+re.getMessage());
    					
    				} catch (InterruptedException ie){
    					if (LockFreeStack.debug) System.out.println("thread "+this.getMyThreadId()+" interrupted!");
    				}
    			}
    		}
    	}
    }

    /**
     * Thread class for testing TwoStackQueueThread
     */
    public static class MyTwoStackQueueThread extends MyThread{
    	public MyTwoStackQueueThread(int id){
    		super(id);
    	}
    	
    	public void run(){
    		Random rand = new Random();
    		for (int i=0; i<numOfQueueOperations; i++){
    			// randomly pick a value
    			int value1 = rand.nextInt(queueValuesBound);
    			int value2 = rand.nextInt(queueValuesBound);
    			// enqueue twice and dequeue once
    			try{
    				queue.enqueue(value1);
    				Thread.sleep(10);
    				queue.enqueue(value2);
    				Thread.sleep(10);
    				queue.dequeue();
    			} catch (InterruptedException ie){
    				if (LockFreeStack.debug) System.out.println("thread "+this.getMyThreadId()+" interrupted!");
    			}
    		}
    	}
    }
}
