package priv.season.coworker.test;


public class A {
	static {
		System.out.println("static init");
	}
	public A() {
		System.out.println("object init");
	}
	public static void sprint() {
		System.out.println("static print");
	}
	
	public void print() {
		System.out.println("object print");
	}
	
	public static double add(Double a, Double b){
		return a+b;
	}
}
