package mpp;

import java.util.concurrent.locks.*;

/**
 * An implementation for the Rooms problem
 * Answer to exercise 97 in the book
 */
public class Rooms {
	// no need for memoy barrier due to lock use
	
	// number of rooms, m in the question
	private int numOfRooms;
	// counter for how many threads are inside the currently active room
	private int threadsInside = 0;
	// waiting queue for all rooms that threads wanted to enter into when another room is active
	private RoomsQueue waitingRooms = new RoomsQueue();
	// currently active room (-1 stands for none)
	private int activeRoom = -1;
	// handlers for all rooms
	private Rooms.Handler[] handlers;
	
	// lock and condition for waiting
	private ReentrantLock lock = new ReentrantLock();
    private Condition[] conditions;
	
    /**
     * interface for room exit handler
     */
	public interface Handler {
		void onEmpty();
	}
	
	/**
	 * default constructor, creates Rooms object with m rooms
	 */
	public Rooms(int m) {
		numOfRooms = m;
		// initialize handlers and conditions
		handlers = new Rooms.Handler[m];
		conditions = new Condition[m];
		for (int i=0; i<m; i++){
			handlers[i] = null;
			conditions[i] = lock.newCondition();
		}
	};
	
	/**
	 * if i is a valid room number, the calling thread will enter room i or
	 * will be inserted to a waiting list until the room's turn is up to be active 
	 */
	public void enter(int i) {
		if (!validateRoom(i)) return;
		
		lock.lock();
		while (true){
			try{				
				// if this is not the first room and either another room is active
				// or the waiting queue is not empty, wait
				while (activeRoom != -1 && (activeRoom != i || !waitingRooms.isEmpty())){
					waitingRooms.enqRoom(i);
					conditions[i].await();
				}
				// room i is done waiting
				// update active room and number of threads in it
				activeRoom = i;
				threadsInside++;
				return;
				
			} catch (InterruptedException ie){
				// interrupted while awaiting, try again
				continue;
			} finally {
				// anyway release the lock
				lock.unlock();
			}
		}
	};
	
	/**
	 * the calling thread will exit the currently active room, and if it's the last
	 * thread to exit, will activate the room handler (if set).
	 * returns true if the exiting thread is the last one in the room and false otherwise.
	 * Assumption: the calling thread is indeed inside the active room
	 */
	public boolean exit() {
		boolean res = false;
		lock.lock();
		try{
			if (threadsInside-- == 0){
				res = true;
				// this is the last thread to exit, run handler
				if (handlers[activeRoom] != null) handlers[activeRoom].onEmpty();
				
				// dequeue next waiting room (if exists) and wake all waiters on it
				int nextRoom = waitingRooms.deqRoom();
				activeRoom = nextRoom; // "none" (-1) or some other room
				if (activeRoom != -1) conditions[activeRoom].signalAll();
			}
		} finally {
			// anyway release the lock
			lock.unlock();
		}
		return res;
	};
	
	/**
	 * if the room number is valid, sets handler h for room i
	 */
	public void setExitHandler(int i, Rooms.Handler h) {
		if (validateRoom(i)) handlers[i] = h;
	};
	
	/**
	 * returns true iff the given room number is valid: 0 <= room number < m
	 */
	public boolean validateRoom(int roomNumber){
		return 0 <= roomNumber && roomNumber < numOfRooms;
	}
	
	/**
	 * class for rooms waiting queue
	 */
	private class RoomsQueue{
		private RoomWaiting head = null;
		
		/**
		 * default constructor
		 */
		public RoomsQueue(){}
		
		/**
		 * inserts the room into the waiting queue if it's not already in it 
		 */
		public void enqRoom(int roomNumber){
			RoomWaiting prevRoom = null;
			RoomWaiting room = head;
			while (room != null){
				if (room.roomNumber == roomNumber) return;
				else {
					prevRoom = room;
					room = room.next;
				}
			}
			// if didn't return by now, insert the room to the end of the queue
			RoomWaiting newRoom = new RoomWaiting(roomNumber);
			if (prevRoom != null) prevRoom.next = newRoom;		// insert at the end of the queue
			else head = newRoom;								// first room to wait in the queue
		}
		
		/**
		 * returns the head of the queue or -1 if empty
		 */
		public int deqRoom(){
			if (head == null) return -1;
			else {
				int headRoom = head.roomNumber;
				head = head.next;
				return headRoom;
			}
		}
		
		/**
		 * returns true iff the room waiting queue is empty
		 */
		public boolean isEmpty(){
			return head == null;
		}
		
		/**
		 * class for rooms waiting in the queue
		 */
		private class RoomWaiting{
			private int roomNumber;
			private RoomWaiting next = null;
			
			public RoomWaiting(int roomNumber){
				this.roomNumber = roomNumber;
			}
		}
	}
}