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

/**
 * Servidor central que coordena pesquisas e coleta estatísticas do sistema.
 * <p>
 * Funcionalidades:
 * <ul>
 *   <li>Balanceamento de carga entre Barrels usando seleção aleatória</li>
 *   <li>Ordenação de resultados por relevância (links de entrada)</li>
 *   <li>Monitoramento em tempo real de Barrels ativos</li>
 *   <li>Cálculo de métricas de desempenho</li>
 * </ul>
 * 
 * @author Miguel Santos
 * @see GatewayRMI
 */
public class Gateway extends UnicastRemoteObject implements GatewayRMI {
    private List<Index> barrels = new ArrayList<>();
    private Random random = new Random();
    private String registryHost;
    private int registryPort;
    private ConcurrentHashMap<String, AtomicLong> searchCounts = new ConcurrentHashMap<>();
    private List<Long> responseTimes = new ArrayList<>();

    /**
     * Construtor que inicializa conexão com o RMI Registry e atualiza Barrels.
     * 
     * @param registryHost Host do RMI Registry (ex: localhost)
     * @param registryPort Porta do RMI Registry (ex: 8183)
     * @throws RemoteException Se falhar exportação do objeto RMI
     */
    public Gateway(String registryHost, int registryPort) throws RemoteException {
        super();
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        refreshBarrels();
    }

    /**
     * Atualiza a lista de Barrels disponíveis dinamicamente.
     * <p>
     * Chamado durante inicialização e após falhas de comunicação.
     */
    private void refreshBarrels() {
        try {
            Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
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

    /**
     * {@inheritDoc}
     * <p>
     * Fluxo de pesquisa:
     * <ol>
     *   <li>Seleciona Barrel aleatório</li>
     *   <li>Recupera URLs associados aos termos</li>
     *   <li>Ordena por links de entrada</li>
     *   <li>Registra tempo de resposta</li>
     * </ol>
     * 
     * @throws RemoteException Se todos Barrels estiverem offline
     */
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

        updateStatistics(query, System.currentTimeMillis() - startTime);
        return results;
    }

    /**
     * Ordena URLs pelo número de links de entrada (relevância).
     * 
     * @param urls Lista de URLs a ordenar
     * @param barrel Barrel usado para consultar links
     * @return Lista ordenada descendente
     */
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

    /**
     * Atualiza estatísticas de uso do sistema.
     * 
     * @param query Termo pesquisado
     * @param responseTime Tempo de resposta em ms
     */
    private void updateStatistics(String query, long responseTime) {
        searchCounts.computeIfAbsent(query, k -> new AtomicLong(0)).incrementAndGet();
        responseTimes.add(responseTime);
        if (responseTimes.size() > 1000) responseTimes.remove(0);
    }

    /**
     * {@inheritDoc}
     * 
     * @param top Número máximo de resultados (recomendado: 10)
     * @return Mapa ordenado por popularidade
     */
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

    /**
     * {@inheritDoc}
     * 
     * @return Tempo médio baseado nas últimas 1000 pesquisas
     */
    @Override
    public double getAverageResponseTime() {
        return responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }

    /**
     * {@inheritDoc}
     * 
     * @return Lista formatada com nome do Barrel e tamanho do índice
     * @throws RemoteException Se não houver Barrels ativos
     */
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

    /**
     * Método de inicialização do Gateway.
     * 
     * @param args Não utilizado
     */
    public static void main(String[] args) {
        try {
            Gateway gateway = new Gateway("localhost", 8183);
            Registry registry = LocateRegistry.getRegistry("localhost", 8183);
            registry.rebind("gateway", gateway);
            System.out.println("Gateway iniciado na porta 8183!");
        } catch (RemoteException e) {
            System.err.println("Erro ao iniciar Gateway: " + e.getMessage());
        }
    }
}