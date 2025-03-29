package search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

/**
 * Interface RMI para operações de indexação e consulta.
 * <p>
 * Contratos obrigatórios:
 * <ul>
 *   <li>Todos os métodos devem ser thread-safe</li>
 *   <li>Exceções RemoteException indicam falhas de rede/IO</li>
 * </ul>
 * 
 * 
 */
public interface Index extends Remote {
    /**
     * Remove e retorna o próximo URL da fila de processamento.
     * 
     * @return URL ou null se fila vazia
     * @throws RemoteException Se falhar comunicação
     */
    String takeNext() throws RemoteException;

    /**
     * Adiciona URL à fila se não existir duplicado.
     * 
     * @param url URL válido 
     * @throws RemoteException Se fila estiver cheia
     */
    void putNew(String url) throws RemoteException;

    /**
     * Associa palavra a um URL no índice invertido.
     * 
     * @param word Palavra normalizada (lowercase)
     * @param url URL onde a palavra ocorreu
     * @throws RemoteException Se índice estiver indisponível
     */
    void addToIndex(String word, String url) throws RemoteException;

    /**
     * Busca URLs associados a uma palavra.
     * 
     * @param word Termo de pesquisa
     * @return Lista de URLs ou lista vazia
     * @throws RemoteException Se ocorrer erro na consulta
     */
    List<String> searchWord(String word) throws RemoteException;

    /**
     * Registra link de origem para um URL de destino.
     * 
     * @param targetUrl URL referenciado
     * @param sourceUrl URL de origem
     * @throws RemoteException Se dados forem inconsistentes
     */
    void addIncomingLink(String targetUrl, String sourceUrl) throws RemoteException;

    /**
     * Retorna links que apontam para um URL.
     * 
     * @param url URL alvo
     * @return Conjunto de URLs de origem
     * @throws RemoteException Se URL não existir
     */
    Set<String> getIncomingLinks(String url) throws RemoteException;

    /**
     * Obtém nome único do Barrel.
     * 
     * @return Nome do serviço (ex: "barrel1")
     * @throws RemoteException Se registro RMI falhar
     */
    String getServiceName() throws RemoteException;

    /**
     * Retorna número de palavras no índice.
     * 
     * @return Tamanho do índice invertido
     * @throws RemoteException Se índice não estiver carregado
     */
    int getIndexSize() throws RemoteException;
}