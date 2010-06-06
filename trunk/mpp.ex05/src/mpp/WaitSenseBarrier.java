package mpp;

import java.util.concurrent.atomic.*;

public class WaitSenseBarrier {
	AtomicInteger count;
	int size;
	boolean sense;
	ThreadLocal<Boolean> threadSense;

	public WaitSenseBarrier(int n) {
		count = new AtomicInteger(n);
		size = n;
		sense = false;
		threadSense = new ThreadLocal<Boolean>() {
			protected Boolean initialValue() { return !sense; };
		};
	}

	public synchronized void await() {
		boolean mySense = threadSense.get();
		int position = count.getAndDecrement();
		if (position == 1) {
			count.set(size);
			sense = mySense;
			notifyAll();
		} else {
			while (sense != mySense) {
				try {
					wait();
				} catch (InterruptedException e) {}
			}
		}
		threadSense.set(!mySense);
	}
}
