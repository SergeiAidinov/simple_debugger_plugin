package target_debug;

import java.time.LocalDateTime;

public class TargetImpl implements Target {
	
	private String world = " world";

	@Override
	public void work() {
		System.out.println(this.getClass() + " started");
		for (int i = 0; i < 50_000_000; i++) {
			sayHello(i);
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void sayHello(int i) {
		String message = " Hello ";
		System.out.println(i + " " + message + world + LocalDateTime.now() + " from " + this.getClass());
	}
	
	private String saySomeThing(String word) {
		return word;
	}

}
