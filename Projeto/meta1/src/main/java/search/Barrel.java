package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Barrel extends UnicastRemoteObject implements Index {

    private Queue<String> urlQueue;
    private Map<String, Set<String>> invertedIndex;

    protected Barrel() throws RemoteException {
        super();
        urlQueue = new LinkedList<>();
        invertedIndex = new HashMap<>();
    }

    @Override
    public synchronized String takeNext() throws RemoteException {
        return urlQueue.poll();
    }

    @Override
    public synchronized void putNew(String url) throws RemoteException {
        if (!urlQueue.contains(url)) {
            urlQueue.offer(url);
        }
    }

    @Override
    public synchronized void addToIndex(String word, String url) throws RemoteException {
        invertedIndex.putIfAbsent(word, new HashSet<>());
        invertedIndex.get(word).add(url);
    }

    @Override
    public synchronized List<String> searchWord(String word) throws RemoteException {
        if (invertedIndex.containsKey(word)) {
            return new ArrayList<>(invertedIndex.get(word));
        }
        return Collections.emptyList();
    }
}


