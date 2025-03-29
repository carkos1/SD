package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Componente responsável por baixar e indexar páginas web de forma distribuída.
 * <p>
 * Funcionalidades:
 * <ul>
 *   <li>Conecta-se dinamicamente a Barrels via RMI Registry</li>
 *   <li>Processa URLs em paralelo usando threads</li>
 *   <li>Extrai texto e links usando Jsoup</li>
 *   <li>Replica dados para múltiplos Barrels para redundância</li>
 * </ul>
 * 
 * @author Igor Reis
 * @see Index
 */
public class Downloader implements Runnable {
    private final String registryHost; 
    private final int registryPort;    
    private List<Index> barrels;       
    private boolean isRunning = true;

    /**
     * Construtor que inicializa conexão com Barrels.
     * 
     * @param registryHost Host do RMI Registry (ex: localhost)
     * @param registryPort Porta do RMI Registry (ex: 8183)
     */
    public Downloader(String registryHost, int registryPort) {
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        this.barrels = new ArrayList<>();
        connectToAllBarrels(); 
    }

    /**
     * Descobre e conecta-se a todos os Barrels registrados no RMI Registry.
     * <p>
     * Atualiza a lista interna de Barrels ativos.
     * Trata falhas de conexão automaticamente.
     */
    private void connectToAllBarrels() {
        try {
            Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
            String[] serviceNames = registry.list();

            for (String name : serviceNames) {
                if (name.startsWith("barrel")) {
                    Index barrel = (Index) registry.lookup(name);
                    barrels.add(barrel);
                    System.out.println("Conectado ao Barrel: " + name);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao conectar aos Barrels: " + e.getMessage());
        }
    }

    /**
     * Loop principal do Downloader. Processa URLs enquanto estiver ativo.
     * <ul>
     *   <li>Obtém URLs de Barrels</li>
     *   <li>Processa cada URL (extração de texto/links)</li>
     *   <li>Dorme se não houver URLs disponíveis</li>
     * </ul>
     */
    @Override
    public void run() {
        while (isRunning) {
            try {
                String url = getNextUrlFromAnyBarrel();
                if (url != null) {
                    processUrl(url);
                } else {
                    Thread.sleep(1000); 
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                isRunning = false; 
                System.out.println("Downloader interrompido.");
            } catch (RemoteException e) {
                System.err.println("Falha na comunicação com o Barrel: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Erro geral: " + e.getMessage());
            }
        }
    }

    /**
     * Obtém o próximo URL disponível de qualquer Barrel.
     * 
     * @return URL válido ou null se nenhum disponível
     * @throws RemoteException Se falhar comunicação com Barrels
     */
    private String getNextUrlFromAnyBarrel() throws RemoteException {
        for (Index barrel : barrels) {
            String url = barrel.takeNext();
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * Processa um URL: indexa palavras e coleta links.
     * 
     * @param url URL a ser processado (ex: "https://example.com")
     * @throws Exception Se falhar download ou análise do HTML
     */
    private void processUrl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                              .userAgent("Mozilla/5.0")
                              .timeout(10000)
                              .get();

            indexText(doc.text(), url);
            collectLinks(doc.select("a[href]"), url);
            System.out.println("Processado: " + url);
        } catch (Exception e) {
            System.err.println("Falha ao processar " + url + ": " + e.getMessage());
        }
    }

    /**
     * Indexa palavras extraídas de uma página.
     * <p>
     * Filtros aplicados:
     * <ul>
     *   <li>Ignora números usando regex</li>
     *   <li>Normaliza para minúsculas</li>
     *   <li>Remove espaços em branco</li>
     * </ul>
     * 
     * @param text Texto bruto da página
     * @param url URL de origem
     */
    private void indexText(String text, String url) {
        String[] words = text.split("\\W+"); 

        for (String word : words) {
            if (!word.isEmpty()) {
                if(word.matches("\\d+")){
                    continue;
                }
                String cleanedWord = word.toLowerCase().trim();
                replicateAddToIndex(cleanedWord, url); 
            }
        }
    }

    /**
     * Coleta e valida links de uma página.
     * 
     * @param links Elementos HTML com links
     * @param sourceUrl URL da página fonte
     */
    private void collectLinks(Elements links, String sourceUrl) {
        for (Element link : links) {
            String targetUrl = link.absUrl("href");
            if (isValidUrl(targetUrl)) {
                replicatePutNew(targetUrl);
                replicateAddIncomingLink(targetUrl, sourceUrl);
            }
        }
    }

    /**
     * Replica links de entrada para todos os Barrels.
     * 
     * @param targetUrl URL de destino
     * @param sourceUrl URL de origem
     */
    private void replicateAddIncomingLink(String targetUrl, String sourceUrl) {
        for (Index barrel : barrels) {
            try {
                barrel.addIncomingLink(targetUrl, sourceUrl);
            } catch (RemoteException e) {
                System.err.println("Falha ao atualizar incoming links: " + e.getMessage());
            }
        }
    }

    /**
     * Valida URLs segundo critérios do sistema.
     * 
     * @param url URL a ser validado
     * @return true se URL é válido e não contém "facebook"
     */
    private boolean isValidUrl(String url) {
        return url.startsWith("http") && !url.contains("facebook");
    }

    /**
     * Replica operação de indexação para todos os Barrels.
     * 
     * @param word Palavra a ser indexada
     * @param url URL associado
     */
    private void replicateAddToIndex(String word, String url) {
        for (Index barrel : barrels) {
            try {
                barrel.addToIndex(word, url);
                } catch (RemoteException e) {
                    System.err.println("Falha ao replicar addToIndex");
            }
        }
    }

    /**
     * Replica operação de adição de URL para todos os Barrels.
     * 
     * @param url URL a ser adicionado
     */
    private void replicatePutNew(String url) {
        for (Index barrel : barrels) {
            try {
                    barrel.putNew(url);
                } catch (RemoteException e) {
                    System.err.println("Falha ao replicar PutNew");
            }
        }
    }

    /**
     * Interrompe a execução do Downloader.
     */
    public void stop() {
        isRunning = false;
    }

    /**
     * Método de inicialização do Downloader.
     * 
     * @param args Não utilizado
     */
    public static void main(String[] args) {
        String host = "192.168.217.173";
        int port = 8183;
        Downloader downloader = new Downloader(host, port);
        new Thread(downloader).start();
    }
}