package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BarrelServer {
    public static void main(String[] args) {
        
        try {
            String serviceName = args[0]; 
            Barrel barrel = new Barrel(serviceName);
            Registry registry = LocateRegistry.getRegistry("192.168.217.173", 8183);
            registry.rebind(serviceName, barrel);
            System.out.println("Barrel registrado: " + serviceName); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}