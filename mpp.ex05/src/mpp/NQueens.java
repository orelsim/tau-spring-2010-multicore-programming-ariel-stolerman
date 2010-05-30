package mpp;

import java.util.concurrent.*;
import java.util.*;

public class NQueens {
	// thread pool
	private final ExecutorService pool;
	// list for the solutions of the last computation
	private List<int[]> solutions = null;
	// the last n the problem was solved for
	private int n;

	/**
	 * default constructor
	 * @param n - number of queens to solve the problem for
	 */
	public NQueens(int n){
		pool = Executors.newCachedThreadPool();
		this.n = n;
		this.solutions = compute();
	}
	
	/**
	 * Computes all solutions for the n-queens problem.
	 * @return a list of all the solutions for the n-queens problem.
	 */
	public List<int[]> compute(){
		List<int[]> res = new ArrayList<int[]>();
		int[] q = new int[n];
		try{
			res = pool.submit(new QueensTask(q, 0)).get();
			pool.shutdown();
		}catch (Exception e){
			System.err.println("Main task failed.");
			return null;
		}
		return res;
	}	
	
	class QueensTask implements Callable<List<int[]>>{
		// members
		private int[] q;	// prefix placing of n queens
		private int n;
		
		/**
		 * default constructor
		 * @param q - prefix placing of n queens
		 * @param n
		 */
		QueensTask(int[] q, int n){
			this.q = q;
			this.n = n;
		}
		
		/**
		 * Calls the computation of solutions for a given placing of n rows of queens 
		 */
		public List<int[]> call(){
			int i = 0;
			int N = q.length;
			List<int[]> res = new ArrayList<int[]>();
			
			// solution found - return it
	        if (n == N){
	        	res.add(q);
	        	return res;
	        }
	        // recursive call over remaining rows
	        else {
	        	List<Future<List<int[]>>> tasks = new ArrayList<Future<List<int[]>>>();  
	            for (i=0; i<N; i++){
	            	int[] copyQ = q.clone();
	            	copyQ[n] = i;
	                if (isConsistent(copyQ, n)) tasks.add(pool.submit(new QueensTask(copyQ,n+1)));
	            }
	            // return results
	            try{
	            	// concatenate results
	            	for (i=0; i<tasks.size(); i++)
	            		res.addAll(tasks.get(i).get());
	            	return res;
	            }catch (Exception e){
	    			System.err.println("Task failed on level "+n+" checking i="+i+".");
	    			return null;
	    		}
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
		String res;
		res = "Found "+solutions.size()+" solutions for "+n+" queens:\n";
		if (solutions.isEmpty()) res += "none!";
		for (int[] sol: solutions){
			res += queensToString(sol);
		}
		return res;
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
		res += "\n";
		return res;
	}
	
	/**
	 * Main for testing
	 */
	public static void main(String[] args) {
		NQueens queens;
		for (int i=1; i<=8; i++){
			queens = new NQueens(i);
			System.out.println(queens);
		}
	}
}
