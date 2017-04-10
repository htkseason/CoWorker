package priv.season.coworker.test;

import java.io.Serializable;

public class TestProcedure implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	double c;


	public TestProcedure(double c) {
		this.c = c;

	}

	public static double sum(double[] arr) throws InterruptedException {
		double result = 0;
		for (double d:arr)
			result+=d;
		return result;
	}
	public double add(double a, double b) throws InterruptedException {
		Thread.sleep(10);
		return a+b+c;
	}

	public static double sadd(double a, double b) {
		return a + b;
	}
}
