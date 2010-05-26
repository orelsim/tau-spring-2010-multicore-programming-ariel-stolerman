package mpp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main class for testing
 */
public class Main {
	public static boolean showQueue = false;
	
	// initialize parameters
	private static int numOfPriorityQueues = 2;						// check PriorityQueue1, PriorityQueue2 
	private static int numOfThreads = 4;							// number of threads
	private static int numOfRuns = 3;								// number of runs to average
	private static MyThread threads[] = new MyThread[numOfThreads];	// threads array
	private static int[] timeouts = {20, 100, 500};					// timeouts
	private static int[] initPriorities = {10, 20, 30};				// initial priorities in queues
	private static PriorityQueue testQueue;							// queue
	private static int N = 20000;									// operations per thread run
	private static int[] failures = new int[numOfThreads];			// failures array per run

	/**
	 * main for testing
	 * @param args
	 */
	public static void main(String[] args) {		
		// timing
		long startTime;
		long endTime;
		
		for (int queueIndex = 0; queueIndex<numOfPriorityQueues; queueIndex++){
			System.out.println("PriorityQueue"+(queueIndex+1)+":");
			System.out.println("###############");
			System.out.println("Initial Priorities\ttime-out (millisecs)\trunning-time (nanosecs)\ttotal failed operations");
			System.out.println("==================\t====================\t=======================\t=======================");
			for(int initPriority: initPriorities){
				for(int timeout: timeouts){
					long avrTime = 0;
					int avrFailures = 0;
					for(int run=0;run<numOfRuns;run++){
						// create current PriorityQueue1 / PriorityQueue2
						if (queueIndex == 0)
							testQueue = new PriorityQueue1(initPriority, TimeUnit.MILLISECONDS, timeout);
						else testQueue = new PriorityQueue2(initPriority, TimeUnit.MILLISECONDS, timeout);
						
						// start timing
						startTime = System.nanoTime();

						for(int i=0; i<numOfThreads; i++) threads[i] = new MyThread(i);
						for(int i=0; i<numOfThreads; i++) threads[i].start();
						try{
							for(int i=0; i<numOfThreads; i++) threads[i].join();
						} catch (InterruptedException ie) {};

						// end timing
						endTime = System.nanoTime();

						avrTime += (endTime-startTime)/numOfRuns;
						
						int sumOfFailures = 0;
						for (int failure: failures)
							sumOfFailures += failure;
						avrFailures += sumOfFailures;
						System.out.println(initPriority+"\t\t\t"+timeout+"\t\t\t"+(endTime-startTime)+"\t\t"+sumOfFailures+" ("+((double)sumOfFailures*100/(double)(numOfThreads*N))+"%)");
					}
					System.out.println("- AVERAGE --------\t--------------------\t-----------------------\t-----------------------");
					avrFailures /= numOfRuns;
					System.out.println(initPriority+"\t\t\t"+timeout+"\t\t\t"+avrTime+"\t\t"+avrFailures+" ("+((double)avrFailures*100/(double)(numOfThreads*N))+"%)");
					System.out.println("------------------\t--------------------\t-----------------------\t-----------------------");
				}
			}
		}
		System.out.println("");
	}

	// MyThread class for testing
	public static class MyThread extends Thread{
		final private int id;
		private int failures = 0;

		public MyThread(int id){
			this.id = id;
		}

		public int getMyThreadID(){
			return id;
		}
		
		public int getNumOfFailures(){
			return failures;
		}

		public void run(){
			int myCounter = 0;
			for (; myCounter < N; myCounter++){
				try {
					int minVal;
					minVal = testQueue.deleteMin();

					// decrease priority by 100
					minVal += 100;

					// put back in the queue
					testQueue.insert(minVal);
				} catch (TimeoutException e) {
					failures++;
				}
				if (myCounter % 1000 == 0 && showQueue){
					int id = ((MyThread)Thread.currentThread()).getMyThreadID();
					System.out.println("Thread "+id+" insert: "+testQueue.toString());
				}
			}
			Main.failures[id] = failures;
		}
	}
	
	// base class for extension by PriorityQueue1 and PriorityQueue2
	public abstract static class PriorityQueue{		
		// abstract methods to be implemented in all priority-queues
		abstract public int deleteMin() throws TimeoutException;
		abstract public void insert(int value) throws TimeoutException;
		abstract public String toString();
	}
}
