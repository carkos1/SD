package search;

import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Robot {
    public static void main(String[] args) {
        try {
            Index index = (Index) LocateRegistry.getRegistry(8183).lookup("index");

            while (true) {
                String url = index.takeNext();
                System.out.println(url);
                Document doc;
                try {
                    doc = Jsoup.connect(url).get();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
    
                String output = doc.text();
                String[] words = output.split("[ ,!.\n();:]");
                
                for (String word: words) {
                    index.addToIndex(word, url);
                }

                Elements links = doc.select("a[href]");

                for (Element link: links) {
                    index.putNew(link.attr("abs:href"));
                }
            }
        } catch (AccessException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}
