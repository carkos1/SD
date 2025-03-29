package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Servidor de armazenamento distribuído com replicação ativa.
 * <p>
 * Estruturas principais:
 * <ul>
 *   <li>urlQueue: Fila thread-safe de URLs pendentes</li>
 *   <li>invertedIndex: HashMap concorrente para índice invertido</li>
 *   <li>incomingLinks: Mapeamento de links entrantes</li>
 * </ul>
 * 
 * @author Igor Reis, Miguel Santos
 * @see Index
 */
public class Barrel extends UnicastRemoteObject implements Index {
    private Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
    private Map<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();
    private static List<Index> replicas = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentHashMap<String, Boolean> urlTracker = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> incomingLinks = new ConcurrentHashMap<>();
    private String serviceName;

    /**
     * Construtor que registra o Barrel no RMI Registry.
     * 
     * @param serviceName Nome único do serviço (ex: "barrel1")
     * @throws RemoteException Se falhar exportação RMI
     */
    public Barrel(String serviceName) throws RemoteException {
        super();
        this.serviceName = serviceName;
        this.urlQueue = new ConcurrentLinkedQueue<>();
        this.invertedIndex = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Remoção atômica da fila e atualização do tracker.
     */
    @Override
    public String takeNext() throws RemoteException {
        String url = urlQueue.poll();
        if (url != null) {
            urlTracker.remove(url);
        }
        return url;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adição condicional com verificação de duplicados.
     */
    @Override
    public void putNew(String url) throws RemoteException {
        if (urlTracker.putIfAbsent(url, Boolean.TRUE) == null) {
            urlQueue.offer(url);
            replicatePutNew(url);
        }
    }

    /**
     * Replica URL para todos os Barrels registrados.
     * 
     * @param url URL a ser replicado
     */
    private void replicatePutNew(String url) {
        for (Index replica : replicas) {
            try {
                replica.putNew(url);
            } catch (RemoteException e) {
                System.err.println("Falha ao replicar URL: " + e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Atualização concorrente do índice invertido.
     */
    @Override
    public void addToIndex(String word, String url) throws RemoteException {
        invertedIndex.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet())
                .add(url);
        replicateAddToIndex(word, url);
    }

    /**
     * Replica operação de indexação para réplicas.
     * 
     * @param word Palavra indexada
     * @param url URL associado
     */
    private void replicateAddToIndex(String word, String url) {
        for (Index replica : replicas) {
            try {
                replica.addToIndex(word, url);
            } catch (RemoteException e) {
                System.err.println("Falha ao replicar índice: " + e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> searchWord(String word) throws RemoteException {
        Set<String> urls = invertedIndex.getOrDefault(word, Collections.emptySet());
        return new ArrayList<>(urls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addIncomingLink(String targetUrl, String sourceUrl) throws RemoteException {
        incomingLinks.computeIfAbsent(targetUrl, k -> ConcurrentHashMap.newKeySet()).add(sourceUrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getIncomingLinks(String url) throws RemoteException {
        return incomingLinks.getOrDefault(url, Collections.emptySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceName() throws RemoteException {
        return this.serviceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexSize() throws RemoteException {
        return invertedIndex.size();
    }
}