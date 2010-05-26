package mpp;

import java.util.concurrent.atomic.*;

public class RoomBasedStack<T> {
	private Rooms rooms;
	
	// stack members
	private AtomicInteger top = new AtomicInteger(0);
	private T[] items;
	
	// Enumerator for push / pop room representation
	private enum Op {
		PUSH(0), POP(1);
		private int value;
		private Op(int val){ this.value = val; }
	}
	
	/**
	 * default constructor with the size of the stack
	 */
	@SuppressWarnings("unchecked")
	public RoomBasedStack(int n){
		this.items = (T[]) new Object[n];
		this.rooms = new MyRooms(2);
	}
	
	/**
	 * push the given element into the stack
	 * throws FullException if the stack is full
	 */
	public void push(T x) throws FullException{
		try{
			rooms.enter(Op.PUSH.value);
			int i = top.getAndIncrement();
			if (i >= items.length) { // stack is full
				top.getAndDecrement(); // restore state
				throw new FullException();
			}
			items[i] = x;
		} finally {
			// anyway leave the push room
			rooms.exit();
		}
	}
	
	/**
	 * pops the top of the stack
	 * throws EmptyException if the stack is empty
	 */
	public T pop() throws EmptyException{
		try{
			rooms.enter(Op.POP.value);
			int i = top.getAndDecrement() - 1;
			if (i < 0) { // stack is empty
				top.getAndIncrement(); // restore state
				throw new EmptyException();
			}
			return items[i];
		} finally {
			// anyway leave the pop room
			rooms.exit();
		}
	}
	
	/**
	 * Rooms interface for RoomBasedStack implementation
	 * Fig. 11.11 in the book
	 */
	public interface Rooms {
		public interface Handler {
			void onEmpty();
		}
		void enter(int i);
		boolean exit();
		public void setExitHandler(int i, Rooms.Handler h) ;
	}
	
	public class MyRooms implements Rooms {
		public MyRooms(int n){
			
		}
		
		@Override
		public void enter(int i) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean exit() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setExitHandler(int i, Handler h) {
			// TODO Auto-generated method stub

		}
	}
}
