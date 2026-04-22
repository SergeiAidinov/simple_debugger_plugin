package target_debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
	
	private static List<String> names = List.of("Jean", "Pierre", "Louis", "Francois", "Henri", "Gabriel", "Rene", "Marcel",
			"Etienne", "Jacques", "Claude", "Bernard", "Luc", "Thierry", "Paul", "Philippe",
			"Alain", "Dominique", "Yves", "Serge", "Georges", "Olivier", "Antoine", "Michel",
			"Gerard", "Christian", "Leon", "Armand", "Benoit", "Raoul", "Robert", "Andre",

			"Marie", "Sophie", "Camille", "Julie", "Claire", "Isabelle", "Nathalie", "Helene",
			"Chantal", "Monique", "Celine", "Elise", "Adele", "Colette", "Brigitte", "Madeleine",
			"Amelie", "Delphine", "Sylvie", "Veronique", "Noelle", "Pauline", "Lucie", "Margot",
			"Jeanne", "Florence", "Sabine", "Anouk", "Estelle", "Manon", "Laure", "Corinne",

			"Hans", "Friedrich", "Karl", "Wilhelm", "Otto", "Hermann", "Dieter", "Klaus",
			"Gunter", "Ulrich", "Manfred", "Rolf", "Wolfgang", "Joachim", "Bernd", "Helmut",
			"Erich", "Lothar", "Rainer", "Stefan", "Thomas", "Andreas", "Jurgen", "Markus",
			"Michael", "Peter", "Sven", "Tobias", "Florian", "Nils", "Dirk", "Heiko",

			"Anna", "Maria", "Ursula", "Petra", "Claudia", "Sabine", "Karin", "Monika",
			"Brigitte", "Renate", "Gisela", "Ingrid", "Helga", "Christina", "Birgit", "Anke",
			"Susanne", "Barbara", "Elke", "Daniela", "Katrin", "Nadine", "Simone", "Heike",
			"Franziska", "Theresa", "Lena", "Leonie", "Marlene", "Johanna", "Paula", "Lisa");
	
	private static List<String> surNames = List.of("Dupont", "Martin", "Bernard", "Dubois", "Moreau", "Laurent", "Simon", "Michel",
			"Leroy", "Roux", "David", "Bertrand", "Thomas", "Robert", "Richard", "Durand",
			"Petit", "Garcia", "Fournier", "Girard", "Andre", "Lefebvre", "Mercier", "Blanc",
			"Chevalier", "Muller", "Fontaine", "Rousseau", "Vincent", "Henry", "Gautier", "Lambert",

			"Bonnet", "Francois", "Legrand", "Garnier", "Faure", "Renaud", "Marchand", "Dumont",
			"Carpentier", "Bourgeois", "Masson", "Boucher", "Giraud", "Colin", "Renard", "Arnaud",
			"Lemoine", "Perrin", "Morin", "Picard", "Caron", "Guillot", "Barbier", "Brun",
			"Prevost", "Perrot", "Rolland", "Clement", "Maillard", "Robin", "Poirier", "Lopez",

			"Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Hoffmann",
			"Schulz", "Koch", "Bauer", "Richter", "Klein", "Wolf", "Neumann", "Schwarz",
			"Zimmermann", "Braun", "Kruger", "Hofmann", "Hartmann", "Lange", "Schmitt", "Werner",
			"Schmitz", "Krause", "Meier", "Lehmann", "Schulze", "Vogel", "Sommer", "Frank",

			"Taylor", "Brown", "Wilson", "Thompson", "Johnson", "White", "Harris", "Martin",
			"Jackson", "Clark", "Lewis", "Walker", "Hall", "Allen", "Young", "King",
			"Wright", "Scott", "Green", "Baker", "Adams", "Nelson", "Hill", "Campbell",
			"Mitchell", "Roberts", "Carter", "Phillips", "Evans", "Turner", "Parker", "Collins");
	
	public static Map<Integer, Tourist> createMapOfTourists() {
		int order = 0;
		Map<Integer, Tourist> tuoristsMap = new HashMap<Integer, Tourist>();
		for (String name : names) {
			for (String surName : surNames) {
				tuoristsMap.put(order, new Tourist(name, surName));
				order++;
			}
		}
		
		
		return tuoristsMap;
	}

}
