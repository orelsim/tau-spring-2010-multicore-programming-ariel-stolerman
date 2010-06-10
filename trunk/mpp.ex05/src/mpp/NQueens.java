package mpp;

import java.util.concurrent.*;
import java.util.*;

public class NQueens {
	// thread pool
	private final ExecutorService pool;
	// solution
	private volatile int[] solution = null; 
	// board size
	private int n;

	/**
	 * default constructor
	 * @param n - number of queens to solve the problem for
	 */
	public NQueens(int n){
		pool = Executors.newCachedThreadPool();
		this.n = n;
		if (n != 2 && n != 3) compute(); // no solutions for 2 and 3
	}
	
	/**
	 * Computes all solutions for the n-queens problem. Waits until a solution is updated.
	 */
	public void compute(){
		int[] q = new int[n];
		int backoff = 1;
		try{
			pool.submit(new QueensTask(q, 0, this));
			while (solution == null) { // wait until a solution is updated
				Thread.sleep(backoff);
				backoff *= 2;
			}
			pool.shutdownNow(); // kill them all!
		}catch (Exception e){
			System.err.println("Main task failed.");
		}
	}	
	
	class QueensTask implements Runnable{
		// members
		private int[] q;	// prefix placing of n queens
		private int n;
		private NQueens queens;
		
		/**
		 * default constructor
		 * @param q - prefix placing of n queens
		 * @param n
		 * @param queens - the NQueens object for solution access
		 */
		QueensTask(int[] q, int n, NQueens queens){
			this.q = q;
			this.n = n;
			this.queens = queens;
		}
		
		/**
		 * Calls the computation of solutions for a given placing of n rows of queens.
		 * if a solution is found, sets the NQueens solution field. 
		 */
		public void run(){
			int i = 0;
			int N = q.length;
			
			// solution found - set it (and hope to win)
	        if (n == N){
	        	queens.solution = q;
	        }
	        // recursive call over remaining rows
	        else {  
	            for (i=0; i<N; i++){
	            	q[n] = i;
	                if (isConsistent(q, n)){
	                	int[] copyQ = q.clone();
	                	pool.submit(new QueensTask(copyQ,n+1, queens));
	                }
	            }
	            q = null;
	        }
		}
		
		/**
		 * Return true if queen placement q[n] does not conflict with
		 * other queens q[0] through q[n-1]
		 * Note: taken from http://www.cs.princeton.edu/introcs/23recursion/Queens.java.html
		 */
		private boolean isConsistent(int[] q, int n) {
	        for (int i = 0; i < n; i++) {
	            if (q[i] == q[n])             return false;   // same column
	            if ((q[i] - q[n]) == (n - i)) return false;   // same major diagonal
	            if ((q[n] - q[i]) == (n - i)) return false;   // same minor diagonal
	        }
	        return true;
	    }
	}

	/* ********************************
	 * for testing and printing results
	 * ********************************/
	
	/**
	 * Returns the string representation of the all the solutions in the last computation done. 
	 */
	public String toString(){
		if (solution == null) return "None!\n";
		else return queensToString(solution);
	}
	
	/**
	 * Returns the string representation of a solution to the queens problem.
	 * @param q - a solution for the queens problem
	 */
	public static String queensToString(int[] q) {
		String res = "";
		int N = q.length;
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (q[i] == j) res += "Q ";
				else res += "* ";
			}
			res += "\n";
		}
		return res;
	}
	
	/**
	 * Main for testing
	 */
	public static void main(String[] args) {
		NQueens queens;
		int threshold = (args.length == 1 ? Integer.parseInt(args[0]) : 13);
		for (int i=1; i<=threshold; i++){
			queens = new NQueens(i);
			System.out.println("Solution for "+i+" queens:\n"+queens);
		}
	}
}
