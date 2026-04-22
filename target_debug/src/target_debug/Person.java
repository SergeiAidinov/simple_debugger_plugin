package target_debug;

import java.util.List;

public class Person {
	
	private static String generalInfo = "just person";
	private final String name;
	private String surname;
	private int mqm = 42;
	private Person friend;
	private int[] intArray = {1, 2, 3};
	private List<String> favoriteAuthors = List.of("Gogol", "Dickens");
	
	public Person(String name, String surname) {
		super();
		this.name = name;
		this.surname = surname;
	}
	public String getName() {
		return name;
	}
	public String getSurname() {
		return surname;
	}
	
	public Person getFriend() {
		return friend;
	}
	public void setFriend(Person friend) {
		this.friend = friend;
	}
	public void introduceYourself() {
		System.out.println("I am " + name + " " + surname);
	}
	
}
