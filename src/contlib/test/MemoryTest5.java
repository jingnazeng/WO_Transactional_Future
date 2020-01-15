package contlib.test;

import contlib.Continuation;

// Tests for memory leaks if no continuations are used
// After the test finishes, the VM should have minimal memory footprint
public class MemoryTest5 implements Runnable {

	private static final int LOOP_ITERS = 50;
	private static final int DEPTH = 30;

	private MemoryTest5() { }

	public void run() {
		doBench(0);
	}

	@SuppressWarnings("unused")
	public void doBench(int depth) {
		long dummy0 = 0;
		long dummy1 = 1;
		long dummy2 = 2;
		int  dummy3 = 3;
		int  dummy4 = 4;
		if (depth < DEPTH) doBench(depth+1);
		else {
			Continuation c = myCapture();
			if (!c.isResumed()) Continuation.resume(c);
		}
	}

	public static void main(String[] args) {
		for (int i = 0; i < LOOP_ITERS; i++) {
			Continuation.runWithContinuationSupport(new MemoryTest5());
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
			//Object last = c.debugBottomRef();
			//String objectStr = last.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(last));
			//System.out.println(c + " bottom is " + objectStr);
		}
		return c;
	}

}
