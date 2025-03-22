package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BarrelServer {
    public static void main(String[] args) {
        try {
            // Create an instance of the Barrel
            Barrel barrel = new Barrel();
            // Create (or get) an RMI registry on a specific port (e.g., 8183)
            Registry registry = LocateRegistry.createRegistry(8183);
            // Bind the barrel to the registry with a unique name
            registry.rebind("barrel1", barrel);
            System.out.println("Barrel server 'barrel1' is ready.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
