package contlib.test;

import contlib.Continuation;

// Tests for memory leaks if no continuations are used
// After the test finishes, the VM should have minimal memory footprint
public class MemoryTest2 implements Runnable {

	private static Continuation _testContinuation;
	private static int _contCounter;
	private static final int LOOP_ITERS = 50;

	private MemoryTest2() { };

	public void run() {
		do {
			_contCounter++;
			_testContinuation = myCapture();
		} while (_testContinuation.isResumed());
	}

	public static void main(String[] args) {
		Continuation.runWithContinuationSupport(new MemoryTest2());

		for (int i = 0; i < LOOP_ITERS; i++) {
			Continuation.runWithContinuationSupport(_testContinuation);
		}

		if (_contCounter != (LOOP_ITERS+1)) {
			System.out.println("ERROR: Got " + _contCounter + " loop iters, expected " + (LOOP_ITERS+1));
		} else {
			System.out.println("TEST OK");
		}

		System.out.println("Finished test");
		while (true) {
			System.runFinalization();
			System.gc();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { throw new Error(e); }
			System.out.print(".");
		}
	}

	public static Continuation myCapture() {
		Continuation c = Continuation.capture();
		if (!c.isResumed()) {
			Object last = c.debugBottomRef();
			String objectStr = last.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(last));
			System.out.println(c + " bottom is " + objectStr);
		}
		return c;
	}

}
