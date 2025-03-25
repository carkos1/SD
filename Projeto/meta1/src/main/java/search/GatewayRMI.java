package search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Interface RMI para operações do Gateway, expondo funcionalidades de pesquisa e monitoramento.
 * <p>
 * Métodos disponíveis para clientes:
 * <ul>
 *   <li>Pesquisa distribuída por termos</li>
 *   <li>Acesso a estatísticas em tempo real</li>
 *   <li>Monitoramento de Barrels ativos</li>
 * </ul>
 * 
 * @author Miguel Santos
 * @see Gateway
 */
public interface GatewayRMI extends Remote {
    /**
     * Realiza uma pesquisa por termos e retorna URLs ordenados por relevância.
     * 
     * @param query Termos de pesquisa (separados por espaço)
     * @return Lista de URLs, no máximo 10 por página
     * @throws RemoteException Se nenhum Barrel estiver disponível ou ocorrer falha de rede
     */
    List<String> search(String query) throws RemoteException;

    /**
     * Retorna os termos mais pesquisados no sistema.
     * 
     * @param top Número máximo de termos a retornar (ex: 10)
     * @return Mapa ordenado (termo, contagem)
     * @throws RemoteException Se não houver dados coletados
     */
    Map<String, Integer> getTopSearches(int top) throws RemoteException;

    /**
     * Lista Barrels ativos com informações de status.
     * 
     * @return Lista formatada: "Nome do Barrel (Tamanho do índice)"
     * @throws RemoteException Se não houver Barrels registrados
     */
    List<String> getActiveBarrels() throws RemoteException;

    /**
     * Calcula o tempo médio de resposta das pesquisas.
     * 
     * @return Média em milissegundos (últimas 1000 pesquisas)
     * @throws RemoteException Se não houver dados suficientes
     */
    double getAverageResponseTime() throws RemoteException;
}