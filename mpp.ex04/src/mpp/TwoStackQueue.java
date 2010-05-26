package mpp;

import mpp.Main.MyThread;

public class TwoStackQueue {
	// for debugging
	public static boolean debug = true;
	
	// members
	// the two stacks to be used
	private LockFreeStack mainStack;
	private LockFreeStack helperStack;
	
	/**
	 * default constructor
	 */
	public TwoStackQueue(){
		mainStack = new LockFreeStack();
		helperStack = new LockFreeStack();
		
		// disable debug messages for class LockFreeStack
		LockFreeStack.debug = false;
	}
	
	/**
	 * inserts the given value into the queue
	 */
	public void enqueue(int value){
		int myId = ((MyThread)Thread.currentThread()).getMyThreadId(); 
			
		boolean doAgain;
		do{
			try {
				mainStack.push(value);
				doAgain = false;
			} catch (RuntimeException re) {
				// mainStack is locked, should try again
				doAgain = true;
			}
		} while (doAgain);
		
		if (debug) System.out.println("thread "+myId+" enqueued "+value+", "+this);
    }

	/**
	 * returns the value at the head of the queue 
	 */
    public int dequeue(){
    	int myId = ((MyThread)Thread.currentThread()).getMyThreadId();
    	int value;
    	
    	// first attain lock on the main stack
    	mainStack.acquire();
    	// spill main stack into helper stack
    	moveAll(mainStack,helperStack);
    	// fetch the top of the helper stack - the head of the queue
    	value = helperStack.pop();
    	// spill all back (except for the head of the queue)
    	moveAll(helperStack,mainStack);
    	// for debugging:
    	String str = this.toString();
    	// release lock
    	mainStack.release();
    	
    	if (debug) System.out.println("thread "+myId+" dequeued "+value+", "+str);
    	return value;
    }
    
    /**
     * pops all elements in the source stack and pushes them into the destination stack
     * such that the top of the source is the bottom of the destination
     * Assumption: called only by locking thread so its push and pop operations will not fail (throw RuntimeException)
     */
    private void moveAll(LockFreeStack source, LockFreeStack dest){
    	int curr = source.pop();
    	while (curr != LockFreeStack.EMPTY){
    		dest.push(curr);
    		curr = source.pop();
    	}
    }
    
    /**
     * string representation for the queue
     */
    public String toString(){
    	LockFreeStack.Elem elem = mainStack.head.getReference();
    	String str = "tail to head: [ ";
    	while (elem != null){
    		str += elem.value+" ";
    		elem = elem.next;
    	}
    	str += "]";
    	return str;
    }
}
