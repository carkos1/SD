package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Servidor de inicialização para instâncias Barrel.
 * <p>
 * Uso: java BarrelServer (nome_serviço)
 * <p>
 * Registra o Barrel no RMI Registry para descoberta dinâmica.
 * 
 * @author Igor Reis, Miguel Santos 
 */
public class BarrelServer {
    /**
     * Inicia um Barrel e o registra no RMI Registry.
     * 
     * @param args nome_serviço (ex: barrel1)
     * @throws IllegalArgumentException Se nome for inválido
     */
    public static void main(String[] args) {
        try {
            String serviceName = args[0];
            Barrel barrel = new Barrel(serviceName);
            Registry registry = LocateRegistry.getRegistry("localhost", 8183);
            registry.rebind(serviceName, barrel);
            System.out.println("Barrel registrado: " + serviceName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}