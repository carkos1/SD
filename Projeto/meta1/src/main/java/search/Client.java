package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
/**
 * Cliente CLI para interação com o sistema Googol.
 * <p>
 * Funcionalidades:
 * <ul>
 *   <li>Menu interativo com 5 opções</li>
 *   <li>Gerenciamento básico de conexões RMI</li>
 *   <li>Exibição formatada de resultados</li>
 * </ul>
 * 
 * @author Igor Reis, Miguel Santos
 */
public class Client {
    /**
     * Método principal que inicia a interface do usuário.
     * 
     * @param args Não utilizado
     */    
    public static void main(String[] args) {
        try {
            // Conecta ao RMI Registry
            Registry registry = LocateRegistry.getRegistry("192.168.217.173", 8183);
            
            // Obtém referência do Gateway
            GatewayRMI gateway = (GatewayRMI) registry.lookup("gateway");
            
            Scanner scanner = new Scanner(System.in);
            int choice;
            
            do {
                System.out.println("\n=== Menu do Cliente ===");
                System.out.println("1. Adicionar URL para indexação");
                System.out.println("2. Pesquisar termos");
                System.out.println("3. Ver links de entrada de uma URL");
                System.out.println("4. Estatísticas do sistema");
                System.out.println("5. Sair");
                System.out.print("Escolha: ");
                choice = scanner.nextInt();
                scanner.nextLine(); // Limpa o buffer

                switch (choice) {
                    case 1:
                        // Adicionar URL (conecta diretamente a um Barrel)
                        System.out.print("Nome do Barrel (ex: barrel1): ");
                        String barrelName = scanner.nextLine();
                        Index barrel = (Index) registry.lookup(barrelName);
                        System.out.print("URL para indexar: ");
                        String url = scanner.nextLine();
                        barrel.putNew(url);
                        System.out.println("URL adicionado à fila!");
                        break;

                    case 2:
                        // Pesquisar termos
                        System.out.print("Termo de pesquisa: ");
                        String query = scanner.nextLine();
                        List<String> results = gateway.search(query);
                        System.out.println("Resultados (" + results.size() + "):");
                        for (String result : results) {
                            System.out.println(" - " + result);
                        }
                        break;

                    case 3:
                        // Links de entrada
                        System.out.print("URL para verificar links de entrada: ");
                        String targetUrl = scanner.nextLine();
                        Index barrelForLinks = (Index) registry.lookup("barrel1"); // Assume barrel1
                        Set<String> incomingLinks = barrelForLinks.getIncomingLinks(targetUrl);
                        System.out.println("Links para " + targetUrl + ":");
                        for (String link : incomingLinks) {
                            System.out.println(" - " + link);
                        }
                        break;

                    case 4:
                        // Estatísticas
                        System.out.println("\n=== Estatísticas ===");
                        Map<String, Integer> topSearches = gateway.getTopSearches(10);
                        System.out.println("Top pesquisas: " + topSearches);

                        List<String> activeBarrels = gateway.getActiveBarrels();
                        System.out.println("Barrels ativos: " + activeBarrels);

                        double avgResponse = gateway.getAverageResponseTime();
                        System.out.println("Tempo médio de resposta: " + avgResponse + " ms");
                        break;

                    case 5:
                        System.out.println("Saindo...");
                        break;

                    default:
                        System.out.println("Opção inválida!");
                }
            } while (choice != 5);

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}