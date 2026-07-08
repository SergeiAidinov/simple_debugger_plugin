package target_debug;

import java.util.List;

public class Tourist {
	
	private final String name;
	private final String surname;
	private List<Tourist> friends;
	
	public Tourist(String name, String surname) {
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
	
	public void setFriends(List<Tourist> friends) {
		this.friends = friends;
	}

	public List<Tourist> getFriends() {
		return friends;
	}
}
