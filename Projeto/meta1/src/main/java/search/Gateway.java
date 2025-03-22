package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Gateway {
    private List<Index> barrels;
    private Random random;

    public Gateway() {
        barrels = new ArrayList<>();
        random = new Random();
        // Lookup available Barrels during initialization
        lookupBarrels();
    }

    private void lookupBarrels() {
        try {
            // Assuming the Barrels are on localhost and port 8183. Adjust if needed.
            Registry registry = LocateRegistry.getRegistry("localhost", 8183);
            // Lookup a known Barrel name
            Index barrel1 = (Index) registry.lookup("barrel1");
            barrels.add(barrel1);
            // If you have more barrels, add additional lookups, e.g.:
            // Index barrel2 = (Index) registry.lookup("barrel2");
            // barrels.add(barrel2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // A simple method to choose a Barrel using random selection
    private Index chooseBarrel() {
        if (barrels.isEmpty()) {
            throw new RuntimeException("No available barrels found.");
        }
        return barrels.get(random.nextInt(barrels.size()));
    }

    // Example method to forward a search request
    public List<String> search(String word) {
        try {
            Index selectedBarrel = chooseBarrel();
            return selectedBarrel.searchWord(word);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Gateway main method to simulate a client query
    public static void main(String[] args) {
        Gateway gateway = new Gateway();
        // Simulate a search query
        List<String> results = gateway.search("example");
        System.out.println("Search results for 'example': " + results);
    }
}
