package contlib.test;

import contlib.Continuation;

public class CreationTest implements Runnable {

	private CreationTest() { };

	private boolean expected = false;

	public void run() {
		Continuation c = Continuation.capture();
		if (c.isResumed() != expected) throw new Error();
		if (!expected) {
			expected = true;
			Continuation.resume(c);
		}
		System.out.println("Test succeeded!");
	}

	public static void main(String[] args) {
		Continuation.runWithContinuationSupport(new CreationTest());
	}

}
