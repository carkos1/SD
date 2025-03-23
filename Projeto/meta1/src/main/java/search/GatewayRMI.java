package search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface GatewayRMI extends Remote {
    List<String> search(String query) throws RemoteException;         
    Map<String, Integer> getTopSearches(int limit) throws RemoteException; 
    List<String> getActiveBarrels() throws RemoteException;            
    double getAverageResponseTime() throws RemoteException;          
}