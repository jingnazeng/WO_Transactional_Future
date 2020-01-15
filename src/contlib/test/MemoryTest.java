package contlib.test;
import contlib.Continuation;

// Tests for memory leaks if no continuations are used
// After the test finishes, the VM should have minimal memory footprint
public class MemoryTest implements Runnable {

	private MemoryTest() { };

	public void run() {
		for (int i = 0; i < 1250000; i++) {
			Continuation.capture();
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

	public static void main(String[] args) {
		Continuation.runWithContinuationSupport(new MemoryTest());
	}

}
