package mpp;

import java.util.concurrent.*;

public class PoolFib
{
	public static void main(String args[])
	{
		System.out.println(Integer.toString(new RunFibonacci().compute(7)));
	}
}

class RunFibonacci
{
	private final ExecutorService pool;

	public RunFibonacci()
	{
		pool = Executors.newCachedThreadPool();
	}

	public int compute( int n )
	{
		int result;
		try {
			result = pool.submit(new FibTask(n)).get().intValue();
			pool.shutdown();
		} catch (Exception e) { return -1; }
		return result;
	}

	class FibTask implements Callable<Integer>
	{
		private int number;

		FibTask(int number)
		{
			this.number = number;
		}

		public Integer call()
		{
			if (number == 0 || number == 1)
				return new Integer(1);
			Future<Integer> task1 = pool.submit(new FibTask(number - 1));
			Future<Integer> task2 = pool.submit(new FibTask(number - 2));
			try {
				return new Integer(task1.get().intValue() + task2.get().intValue());
			} catch (Exception e) { return -1; }
		}
	}
}