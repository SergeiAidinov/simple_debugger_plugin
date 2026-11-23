package target_debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestDataProvider {
	
	private final Random random = new Random();
	private Map<Integer, Tourist> mapOfTourists; 
	private final Map<String, List<String>> attractionsByCountry =
	        createAttractionsByCountry();
	
	private  List<String> names = List.of("Jean", "Pierre", "Louis", "Francois", "Henri", "Gabriel", "Rene",
			"Marcel", "Etienne", "Jacques", "Claude", "Bernard", "Luc", "Thierry", "Paul", "Philippe", "Alain",
			"Dominique", "Yves", "Serge", "Georges", "Olivier", "Antoine", "Michel", "Gerard", "Christian", "Leon",
			"Armand", "Benoit", "Raoul", "Robert", "Andre",

			"Marie", "Sophie", "Camille", "Julie", "Claire", "Isabelle", "Nathalie", "Helene", "Chantal", "Monique",
			"Celine", "Elise", "Adele", "Colette", "Brigitte", "Madeleine", "Amelie", "Delphine", "Sylvie", "Veronique",
			"Noelle", "Pauline", "Lucie", "Margot", "Jeanne", "Florence", "Sabine", "Anouk", "Estelle", "Manon",
			"Laure", "Corinne",

			"Hans", "Friedrich", "Karl", "Wilhelm", "Otto", "Hermann", "Dieter", "Klaus", "Gunter", "Ulrich", "Manfred",
			"Rolf", "Wolfgang", "Joachim", "Bernd", "Helmut", "Erich", "Lothar", "Rainer", "Stefan", "Thomas",
			"Andreas", "Jurgen", "Markus", "Michael", "Peter", "Sven", "Tobias", "Florian", "Nils", "Dirk", "Heiko",

			"Anna", "Maria", "Ursula", "Petra", "Claudia", "Sabine", "Karin", "Monika", "Brigitte", "Renate", "Gisela",
			"Ingrid", "Helga", "Christina", "Birgit", "Anke", "Susanne", "Barbara", "Elke", "Daniela", "Katrin",
			"Nadine", "Simone", "Heike", "Franziska", "Theresa", "Lena", "Leonie", "Marlene", "Johanna", "Paula",
			"Lisa");

	private List<String> surNames = List.of("Dupont", "Martin", "Bernard", "Dubois", "Moreau", "Laurent",
			"Simon", "Michel", "Leroy", "Roux", "David", "Bertrand", "Thomas", "Robert", "Richard", "Durand", "Petit",
			"Garcia", "Fournier", "Girard", "Andre", "Lefebvre", "Mercier", "Blanc", "Chevalier", "Muller", "Fontaine",
			"Rousseau", "Vincent", "Henry", "Gautier", "Lambert",

			"Bonnet", "Francois", "Legrand", "Garnier", "Faure", "Renaud", "Marchand", "Dumont", "Carpentier",
			"Bourgeois", "Masson", "Boucher", "Giraud", "Colin", "Renard", "Arnaud", "Lemoine", "Perrin", "Morin",
			"Picard", "Caron", "Guillot", "Barbier", "Brun", "Prevost", "Perrot", "Rolland", "Clement", "Maillard",
			"Robin", "Poirier", "Lopez",

			"Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Hoffmann", "Schulz", "Koch",
			"Bauer", "Richter", "Klein", "Wolf", "Neumann", "Schwarz", "Zimmermann", "Braun", "Kruger", "Hofmann",
			"Hartmann", "Lange", "Schmitt", "Werner", "Schmitz", "Krause", "Meier", "Lehmann", "Schulze", "Vogel",
			"Sommer", "Frank",

			"Taylor", "Brown", "Wilson", "Thompson", "Johnson", "White", "Harris", "Martin", "Jackson", "Clark",
			"Lewis", "Walker", "Hall", "Allen", "Young", "King", "Wright", "Scott", "Green", "Baker", "Adams", "Nelson",
			"Hill", "Campbell", "Mitchell", "Roberts", "Carter", "Phillips", "Evans", "Turner", "Parker", "Collins");
	
	private Map<String, List<String>> createAttractionsByCountry() {

	    Map<String, List<String>> attractions = new LinkedHashMap<>();

	    attractions.put("France", List.of(
	            "Eiffel Tower",
	            "Louvre Museum",
	            "Palace of Versailles",
	            "Mont Saint-Michel",
	            "Carcassonne"
	    ));

	    attractions.put("Italy", List.of(
	            "Colosseum",
	            "Pompeii",
	            "Leaning Tower of Pisa",
	            "St. Peter's Basilica",
	            "Valley of the Temples"
	    ));

	    attractions.put("Germany", List.of(
	            "Brandenburg Gate",
	            "Neuschwanstein Castle",
	            "Cologne Cathedral",
	            "Reichstag Building",
	            "Museum Island"
	    ));

	    attractions.put("Spain", List.of(
	            "Sagrada Familia",
	            "Alhambra",
	            "Park Güell",
	            "Prado Museum",
	            "Mosque-Cathedral of Córdoba"
	    ));

	    attractions.put("Greece", List.of(
	            "Acropolis of Athens",
	            "Delphi",
	            "Meteora",
	            "Palace of Knossos",
	            "Ancient Olympia"
	    ));

	    attractions.put("Japan", List.of(
	            "Mount Fuji",
	            "Himeji Castle",
	            "Kiyomizu-dera Temple",
	            "Fushimi Inari Shrine",
	            "Hiroshima Peace Memorial"
	    ));

	    attractions.put("Egypt", List.of(
	            "Pyramids of Giza",
	            "Great Sphinx",
	            "Valley of the Kings",
	            "Karnak Temple",
	            "Abu Simbel Temples"
	    ));

	    return attractions;
	}
	
	private List<String> getAllAttractions() {
	    return attractionsByCountry.values().stream()
	            .flatMap(List::stream)
	            .toList();
	}
	
	public TestDataProvider() {
		createMapOfTourists();
	}
	
	private Map<String, List<String>> createAttractionPreferences() {

	    List<String> attractions = new ArrayList<>(getAllAttractions());
	    Collections.shuffle(attractions, random);

	    int visitedCount = random.nextInt(attractions.size() + 1);
	    int wantedCount = random.nextInt(attractions.size() - visitedCount + 1);

	    List<String> visited = new ArrayList<>(
	            attractions.subList(0, visitedCount));

	    List<String> wanted = new ArrayList<>(
	            attractions.subList(visitedCount, visitedCount + wantedCount));

	    List<String> unwanted = new ArrayList<>(
	            attractions.subList(visitedCount + wantedCount, attractions.size()));

	    Map<String, List<String>> preferences = new LinkedHashMap<>();
	    preferences.put("Visited attractions", visited);
	    preferences.put("Wants to visit", wanted);
	    preferences.put("Does not want to visit", unwanted);

	    return preferences;
	}
	
	private Map<String, List<String>> createAttractionsByCountryCopy() {
	    Map<String, List<String>> copy = new LinkedHashMap<>();

	    attractionsByCountry.forEach((country, attractions) ->
	            copy.put(country, new ArrayList<>(attractions)));

	    return copy;
	}
	
	private Map<String, Object> createTouristAttractions() {

	    Map<String, Object> attractions = new LinkedHashMap<>();

	    attractions.putAll(createAttractionPreferences());
	    attractions.put("Attractions by country", createAttractionsByCountryCopy());

	    return attractions;
	}

	public  List<Tourist> getListOfTourists() {
		return  mapOfTourists.values().stream().toList();
	}
	
	public Map<Integer, Tourist> getMapOfTourists() {
		return mapOfTourists;
	}
	
	public void createMapOfTourists() {
		Map<Integer, Tourist> tuoristsMap = new HashMap<Integer, Tourist>();
		List<Tourist> touristList = new ArrayList<Tourist>();
		for (String name : names) {
			for (String surName : surNames) {
				touristList.add(new Tourist(name, surName, createTouristAttractions()));
			}
		}
		Collections.shuffle(touristList);
		touristList = defineFriends(touristList);
		for (int i = 0; i < touristList.size(); i++) {
			tuoristsMap.put(i, touristList.get(i));
		}
		mapOfTourists = tuoristsMap;
	}
	
	private List<Tourist> defineFriends(List<Tourist> touristList) {
	    int head = 0;
	    while (head < touristList.size()) {
	        int groupSize = random.nextInt(5) + 2;
	        int tail = Math.min(head + groupSize, touristList.size());
	        if (touristList.size() - tail == 1) {
	            tail = touristList.size();
	        }
	        List<Tourist> group = touristList.subList(head, tail);
	        for (Tourist tourist : group) {
	            List<Tourist> friends = new ArrayList<>(group);
	            friends.remove(tourist);
	            tourist.setFriends(friends);
	        }
	        head = tail;
	    }
	    return touristList;
	}

}
