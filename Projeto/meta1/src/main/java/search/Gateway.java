package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Gateway extends UnicastRemoteObject implements GatewayRMI {
    private List<Index> barrels = new ArrayList<>();
    private Random random = new Random();
    private String registryHost;
    private int registryPort;
    private ConcurrentHashMap<String, AtomicLong> searchCounts = new ConcurrentHashMap<>(); 
    private List<Long> responseTimes = new ArrayList<>(); 

    public Gateway(String registryHost, int registryPort) throws RemoteException {
        super();
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        refreshBarrels();
    }

    //Vê quais os Barrels disponíveis 
    private void refreshBarrels() {
        try {
            Registry registry = LocateRegistry.getRegistry("51.21.207.175", 8183);
            String[] serviceNames = registry.list();
            List<Index> newBarrels = new ArrayList<>();

            System.out.println("Serviços registrados: " + Arrays.toString(serviceNames));

            for (String name : serviceNames) {
                if (name.startsWith("barrel")) {
                    Index barrel = (Index) registry.lookup(name);
                    newBarrels.add(barrel);
                }
            }

            this.barrels = newBarrels;
            System.out.println("Barrels atualizados: " + barrels.size());

        } catch (Exception e) {
            System.err.println("Falha ao atualizar Barrels: " + e.getMessage());
        }
    }

    //Vai, escolhe um barril aleatório e manda para a função para ordenar os URLS
    @Override
    public List<String> search(String query) throws RemoteException {
        long startTime = System.currentTimeMillis();
        
        if (barrels.isEmpty()) {
            refreshBarrels();
            if (barrels.isEmpty()) return Collections.emptyList();
        }

        Index barrel = barrels.get(random.nextInt(barrels.size())); 
        List<String> results;

        try {
            results = barrel.searchWord(query);
            results = sortByIncomingLinks(results, barrel); 
        } catch (RemoteException e) {
            System.err.println("Falha no Barrel. Atualizando lista...");
            refreshBarrels();
            return search(query);
        }

        updateStatistics(query, System.currentTimeMillis() - startTime); // Atualiza estatísticas
        return results;
    }

    // Ordena URLs pelo número de links de entrada (relevância)
    private List<String> sortByIncomingLinks(List<String> urls, Index barrel) {
        urls.sort((url1, url2) -> {
            try {
                int links1 = barrel.getIncomingLinks(url1).size();
                int links2 = barrel.getIncomingLinks(url2).size();
                return Integer.compare(links2, links1);
            } catch (RemoteException e) {
                return 0;
            }
        });
        return urls;
    }

    //Conta num de pesquisas e tempo de resposta
    private void updateStatistics(String query, long responseTime) {
        searchCounts.computeIfAbsent(query, k -> new AtomicLong(0)).incrementAndGet();
        responseTimes.add(responseTime);
        if (responseTimes.size() > 1000) responseTimes.remove(0);
    }

    // Devolve as n pesquisas mais frequentes
    @Override
    public Map<String, Integer> getTopSearches(int top) {
        return searchCounts.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(top)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().intValue()
            ));
    }

    // Devolve o tempo de resposta
    @Override
    public double getAverageResponseTime() {
        return responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }

    //Mostra os barris que estão atívos e a funfar
    @Override
    public List<String> getActiveBarrels() throws RemoteException {
        return barrels.stream()
            .map(barrel -> {
                try {
                    return barrel.getServiceName() + " (Index size: " + barrel.getIndexSize() + ")";
                } catch (RemoteException e) {
                    return "Barrel inacessível";
                }
            })
            .collect(Collectors.toList());
    }


    // Regista o Gateway no RMI para que haja comunicação
    public static void main(String[] args) {
    try {
        System.setProperty("java.rmi.server.hostname", "51.21.207.175");
        Gateway gateway = new Gateway("51.21.207.175", 8183); 
        Registry registry = LocateRegistry.getRegistry("51.21.207.175", 8183);
        registry.rebind("gateway", gateway);
        System.out.println("Gateway iniciado na porta 8183!");
    } catch (RemoteException e) {
        System.err.println("Erro ao iniciar Gateway: " + e.getMessage());
    }
}
}
