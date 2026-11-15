package target_debug;

import java.util.List;
import java.util.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hash(name, surname);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tourist other = (Tourist) obj;
		return Objects.equals(name, other.name) && Objects.equals(surname, other.surname);
	}
}
