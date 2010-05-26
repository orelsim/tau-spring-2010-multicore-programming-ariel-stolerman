package mpp;

import java.util.concurrent.atomic.*;
import mpp.Main.MyThread;

public class LockFreeStack {
	// for debugging
	public static boolean debug = true;
	
	public final static int NONE = -1;
	public final static int EMPTY = -1;
	
	// members
	protected AtomicStampedReference<Elem> head;
	
	/**
	 * default constructor
	 */
	public LockFreeStack(){
		head = new AtomicStampedReference<Elem>(null, NONE);
	}
	
	/**
	 * pushes the given value into the stack
	 * throws RuntimeException if the stack is acquired by some other thread
	 */
	public void push(int value){
		Elem newHead = new Elem(value);
		Elem currHead;
		int[] currStamp = new int[1];
		boolean succeeded;
		
		do{
			// check if locked by other
			if (!isFreeOrLockedByMe())
				throw new RuntimeException("Thread "+getMyThreadId()+": Cannot push "+value+", the stack is locked.");
			// get head and stamp
			currHead = head.get(currStamp);
			// point my next to current head and try to be the new head while keeping correctness
			newHead.next = currHead;
			succeeded = head.compareAndSet(currHead, newHead, currStamp[0], currStamp[0]);
		} while (!succeeded);
		
		if (debug) System.out.println("after thread "+getMyThreadId()+" pushed "+value+", "+this);
    }
	
	/**
	 * pops the value at the top of the stack and returns it, or returns -1 if empty.
	 * throws RuntimeException if the stack is acquired by some other thread
	 */
    public int pop(){
    	Elem currHead;
    	Elem nextHead;
    	int[] currStamp = new int[1];
		boolean succeeded;
		do{
			// check if locked by other
			if (!isFreeOrLockedByMe())
				throw new RuntimeException("Thread "+getMyThreadId()+": Cannot pop, the stack is locked.");
			// get head and stamp
			currHead = head.get(currStamp);
			// if head is null return "empty"
			if (currHead == null){
				if (debug) System.out.println("thread "+getMyThreadId()+" tried to pop, stack is EMPTY");
				return EMPTY;
			}
			// try to set head to head->next while keeping correctness
			nextHead = currHead.next;
			succeeded = head.compareAndSet(currHead, nextHead, currStamp[0], currStamp[0]);
		} while (!succeeded);
		
		// return the popped value
		if (debug) System.out.println("after thread "+getMyThreadId()+" popped "+currHead.value+", "+this);
		return currHead.value;
    }
    
    /**
     * Acquires the stack
     * after acquired no thread other than the acquiring thread can change the stack
     */
    public void acquire(){
    	Elem currHead;
    	boolean succeeded;
    	
    	// try to stamp the head until succeeded
    	do {
    		currHead = head.getReference();
    		// stamp it only if it's free
    		succeeded = head.compareAndSet(currHead, currHead, NONE, getMyThreadId());
    	} while (!succeeded);
    }
    
    /**
     * releases the stack ONLY if the acquiring thread is the calling one
     */
    public void release(){
    	Elem currHead = head.getReference();
    	head.compareAndSet(currHead, currHead, getMyThreadId(), NONE);
    }
    
    /* ****************
     * private methods
     ******************/
    
    /**
     * returns the calling thread's id
     */
    private int getMyThreadId(){
    	return ((MyThread)Thread.currentThread()).getMyThreadId();
    }
    
    /**
     * returns true iff the stack is locked by the calling thread or not locked at all
     */
    private boolean isFreeOrLockedByMe(){
    	return head.getStamp() == getMyThreadId() || head.getStamp() == NONE; 
    }
    
    /**
     * String representation of the stack
     */
    public String toString(){
    	int stamp = head.getStamp();
    	String stack = "locker: "+(stamp == NONE ? "none" : stamp)+", elements: [ ";
    	Elem elem = head.getReference();	
    	while (elem != null){
    		stack += elem.value+" ";
    		elem = elem.next;
    	}
    	stack += "]";
    	return stack;
    }
	
	
    /**
     * stack elements class
     */
    protected static class Elem{
    	// members
    	protected int value;
    	protected Elem next = null;
    	
    	/**
    	 * default constructor from the value and the previous element in the stack
    	 */
    	public Elem(int value){
    		this.value = value;
    	}
    }
}