package contlib.test;

import contlib.Continuation;

// Tests for memory leaks if no continuations are used
// After the test finishes, the VM should have minimal memory footprint
public class MemoryTest3 implements Runnable {

	private static int _contCounter;
	private static final int LOOP_ITERS = 50;

	private MemoryTest3() {
		System.out.println(toString());
	};

	public void run() {
		do {
			Continuation c = myCapture();
			if (!c.isResumed()) Continuation.resume(c);
			_contCounter++;
		} while (_contCounter < LOOP_ITERS);
	}

	public static void main(String[] args) {
		Continuation.runWithContinuationSupport(new MemoryTest3());

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
