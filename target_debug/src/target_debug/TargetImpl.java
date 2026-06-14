package target_debug;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TargetImpl implements Target {
	
	private String world = " world";
	//private static String  staticString = "I am a static field!";
//	private List<Integer> integers = List.of(1,2,3);
//	private Set<String> strings = Set.of("One", "Two", "Three");
	private Person person1 = new Person("Sergei", "Aidinov");
	private Person person2 = new Person("Ethan", "Caldwell");
	Map<String, Person> fieldPersonsMap = Map.of("Sergei", person1,
			"Ethan", person2);
	Set<Integer> longIntegerSet = compileSet();
	Map<Integer, Tourist> tourists = Utils.createMapOfTourists();
	
	@Override
	public void work() {
		System.out.println(this.getClass() + " started");
		System.out.println(person1);
		System.out.println(person2);
		person1.setFriend(person2);
		person2.setFriend(person1);
		person1.introduceYourself();
		person2.introduceYourself();
		Map<String, Person> persons = Map.of("Sergei", person1,
				"Ethan", person2);
		List<Person> personList = List.of(person1, person2);
		System.out.println("CouCou!");
//		strings.stream().forEach(e -> System.out.println(e));
//		integers.stream().forEach(e -> System.out.println(e));
		persons.entrySet().stream().forEach(e -> System.out.println(e));
		
		for (int i = 0; i < 50_000_000; i++) {
			System.out.println(i);
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
	
	private Set<Integer> compileSet() {
		Set<Integer> set = new HashSet();
		for (int i = -32768; i < 32768; i++) {
			set.add(new Integer(i));
		}
		return set;
	}
	
	

}
