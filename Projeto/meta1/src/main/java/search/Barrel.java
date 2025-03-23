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

public class Barrel extends UnicastRemoteObject implements Index {

    //Fila de URLS Concurrent garante que os downloaders acessem a fila sem corromper para ser Thread-Safe
    private Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
    
    //Índice invertido que tmb é concurrent pq prontos Thread-Safe
    private Map<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();

    //Lista com réplicas dos barrels para não haver perda de memoria (reliable multicast)
    private static List<Index> replicas = Collections.synchronizedList(new ArrayList<>());

    //Criamos o urlTracker para depois, quando for a colocar usamo-lo para comparar
    private ConcurrentHashMap<String, Boolean> urlTracker = new ConcurrentHashMap<>();

    //Lista com os links
    private ConcurrentHashMap<String, Set<String>> incomingLinks = new ConcurrentHashMap<>();

    private String serviceName;


    public Barrel(String serviceName) throws RemoteException {
        super();
        this.serviceName = serviceName;
        urlQueue = new ConcurrentLinkedQueue<>();  
        invertedIndex = new ConcurrentHashMap<>(); 
    }

    //faz "pop" ao URL os downloaders assim buscam os URLS poll é um método atómico logo não há risco de 2 downloaders receberem o mesmo URL limpa-o tmb do urlTracker para dps o podermos voltar a meter
    @Override
    public String takeNext() throws RemoteException {
        String url = urlQueue.poll();
        if (url != null){
            urlTracker.remove(url);
        }
        return url;
    }


    //Mete na fila não inclui duplicados e replica nos outros barrels (cliente pode adicionar) o putIfAbsent é atómico enquanto o contains n é atómico devolve null se n existia URL e devolve Boolean.TRUE caso exista
    @Override
    public void putNew(String url) throws RemoteException {
            if (urlTracker.putIfAbsent(url, Boolean.TRUE) == null) {
            urlQueue.offer(url);
            replicatePutNew(url);
        }
    }


    private void replicatePutNew(String url) {
        for (Index replica : replicas) {
            try {
                replica.putNew(url); 
            } catch (RemoteException e) {
                System.err.println("Falha ao replicar URL: " + e.getMessage());
            }
        }   
    }


    //Adiciona palavra ao index e mete nos outro Barrels ñ precisa de sync pq compute... e add já são atómicos
    @Override
    public void addToIndex(String word, String url) throws RemoteException {
        invertedIndex.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet())
                .add(url);

        replicateAddToIndex(word, url);
    }

    private void replicateAddToIndex(String word, String url) {
    for (Index replica : replicas) {
        try {
            replica.addToIndex(word, url);
        } catch (RemoteException e) {
            System.err.println("Falha ao replicar índice: " + e.getMessage());
        }
    }
}

    //Devolve o URL encontrado através da word caso exista, senão devolve default (getOrDefault) devolve um novo array pro cliente n mudar td
    @Override
    public List<String> searchWord(String word) throws RemoteException {
        Set<String> urls = invertedIndex.getOrDefault(word, Collections.emptySet());
        return new ArrayList<>(urls);
    }

    //Adiciona o link
    @Override
    public void addIncomingLink(String targetUrl, String sourceUrl) throws RemoteException {
        incomingLinks.computeIfAbsent(targetUrl, k -> ConcurrentHashMap.newKeySet()).add(sourceUrl);
    }

    @Override
    public Set<String> getIncomingLinks(String url) throws RemoteException {
        return incomingLinks.getOrDefault(url, Collections.emptySet());
    }

    @Override
    public String getServiceName() throws RemoteException {
        return this.serviceName;
    }

    @Override
    public int getIndexSize() throws RemoteException {
        return invertedIndex.size();
    }
}


