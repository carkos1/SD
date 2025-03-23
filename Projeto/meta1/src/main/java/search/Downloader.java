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


public class Downloader implements Runnable {
    private final String registryHost; 
    private final int registryPort;    
    private List<Index> barrels;       
    private boolean isRunning = true;

    public Downloader(String registryHost, int registryPort) {
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        this.barrels = new ArrayList<>();
        connectToAllBarrels(); 
    }

    // Conecta a tds os Barrels para ter Multicast e redundância vai ao registo do RMI mete os barris todos numa lista de barris (inserir meme do gato aq)

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

    //Ação mesmo de ir buscar os URLS aos Barrels
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

    // Vai aos barris e aquele q tiver um URL disonível "traz pa casa"
    private String getNextUrlFromAnyBarrel() throws RemoteException {
        for (Index barrel : barrels) {
            String url = barrel.takeNext();
            if (url != null) {
                return url;
            }
        }
        return null;
    }


    // Vai ao URL e indexa as palavras e busca os links
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

    // Mete as palavras "bunitas" e nos barrels o regex \\W+ significa caracter alfanumérico ou seja tudo oq for letras e números contam para as palavras se detetarmos um digito d+ passamos à frente

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

    // mete os links no barrel
    private void collectLinks(Elements links, String sourceUrl) {
        for (Element link : links) {
            String targetUrl = link.absUrl("href");
            if (isValidUrl(targetUrl)) {
                replicatePutNew(targetUrl);
                replicateAddIncomingLink(targetUrl, sourceUrl);
            }
        }
    }
    // Mete links nos outros Barrels
    private void replicateAddIncomingLink(String targetUrl, String sourceUrl) {
        for (Index barrel : barrels) {
            try {
                barrel.addIncomingLink(targetUrl, sourceUrl);
            } catch (RemoteException e) {
                System.err.println("Falha ao atualizar incoming links: " + e.getMessage());
            }
        }
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http") && !url.contains("facebook");
    }

    // Mete indices nos outros Barrels (URLS são os endereços primários, links são as referências a endereços dentro do site)
    private void replicateAddToIndex(String word, String url) {
        for (Index barrel : barrels) {
            try {
                barrel.addToIndex(word, url);
                } catch (RemoteException e) {
                    System.err.println("Falha ao replicar addToIndex");
            }
        }
    }

    // Mete URLS nos outros Barrels (URLS são os endereços primários, links são as referências a endereços dentro do site)
    private void replicatePutNew(String url) {
        for (Index barrel : barrels) {
            try {
                    barrel.putNew(url);
                } catch (RemoteException e) {
                    System.err.println("Falha ao replicar PutNew");
            }
        }
    }

    //interrompe o downloader
    public void stop() {
        isRunning = false;
    }

    //inicia o downloader
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8183;
        Downloader downloader = new Downloader(host, port);
        new Thread(downloader).start();
    }
}