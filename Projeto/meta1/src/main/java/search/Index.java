package search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

public interface Index extends Remote {
    String takeNext() throws RemoteException;
    void putNew(String url) throws RemoteException;
    void addToIndex(String word, String url) throws RemoteException;
    List<String> searchWord(String word) throws RemoteException;
    void addIncomingLink(String targetUrl, String sourceUrl) throws RemoteException;
    Set<String> getIncomingLinks(String url) throws RemoteException;
    String getServiceName() throws RemoteException;
    int getIndexSize() throws RemoteException;
}