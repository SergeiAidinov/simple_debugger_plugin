package target_debug;

import java.util.List;



public class Main {
//	private static String  staticString = "I am a static field!";
//	static List<Integer> initList = List.of(4 ,5 ,7);

	public static void main(String[] args) {
		System.out.println("Hello");
	//	for (Integer integer : initList) System.out.println(integer);
		new TargetImpl().work();

	}

}
