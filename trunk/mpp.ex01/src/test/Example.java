package test;

public class Example
{
    public static void main(String args[])
    {
        System.out.println("Starting...");
        
        MyThread threads[] = new MyThread[2];

        int i;
		long start = System.nanoTime();

        for (i = 0; i < 2; i++)
            threads[i] = new MyThread(i);
        for (i =0; i < 2; i++)
            threads[i].start();
        try {
            for (i =0; i < 2; i++)
                threads[i].join();
        }
        catch (InterruptedException e) { };
		
        System.out.format("Finished after %d miliseconds.", 
			new Long((System.nanoTime() - start) / 1000000));
    }
	
	private static class MyThread extends Thread
	{
	    final private int arg;

	    public MyThread(int arg)
	    {
	        this.arg = arg;
	    }

	    public void run()
	    {
	        try {
	            Thread.sleep(1000);
	        }
	        catch (InterruptedException e) { };
	        System.out.println("thread " + this.arg + " done");
	    }
	}
}