package mpp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
//TODO add volotiles?
/**
 *	Implementation of the Time-Out Lock
 */
public class TOLock {
	
	// static members
	static QNode AVAILABLE = new QNode();
	
	// virtual members
	AtomicReference<QNode> tail;
	ThreadLocal<QNode> myNode;
	
	/**
	 * default constructor
	 */
	public TOLock() {
		tail = new AtomicReference<QNode>(null);
		myNode = new ThreadLocal<QNode>() {
			protected QNode initialValue() {
				return new QNode();
			}
		};
	}

	/**
	 * Time-Out Lock locking method
	 * @param time
	 * 				The maximum time to wait for the lock to be attained
	 * @param unit
	 * 				The time units for the time parameter
	 * @return
	 * 				If the lock is attained in the time frame given, returns true.
	 * 				Otherwise returns false.
	 * @throws InterruptedException
	 */
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		long patience = TimeUnit.MILLISECONDS.convert(time, unit);
		QNode qnode = new QNode();
		myNode.set(qnode);
		qnode.pred = null;
		QNode myPred = tail.getAndSet(qnode);
		if (myPred == null || myPred.pred == AVAILABLE) {
			return true;
		}
		while (System.currentTimeMillis() - startTime < patience) {
			QNode predPred = myPred.pred;
			if (predPred == AVAILABLE) {
				return true;
			} else if (predPred != null) {
				myPred = predPred;
			}
		}
		if (!tail.compareAndSet(qnode, myPred))
			qnode.pred = myPred;
		return false;
	}
	
	/**
	 * Time-Out Lock unlocking method
	 */
	public void unlock() {
		QNode qnode = myNode.get();
		if (!tail.compareAndSet(qnode, null))
			qnode.pred = AVAILABLE;
	}

	/**
	 * Class QNode for holding the nodes for each thread with a pointer to the predecessor
	 */
	static class QNode {
		public QNode pred = null;
	}
}
