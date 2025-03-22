package search;

import java.io.IOException;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Downloader {

    // Set to track already visited URLs (to prevent duplicate processing)
    private static HashSet<String> visitedUrls = new HashSet<>();

    public static void main(String[] args) {
        // Starting URL (seed)
        String seedUrl = "https://www.uc.pt";
        downloadPage(seedUrl);
    }

    /**
     * Downloads a page from the given URL, prints the title, and extracts links.
     * @param url the URL to download
     */
    public static void downloadPage(String url) {
        // Skip if already visited
        if (visitedUrls.contains(url)) {
            System.out.println("Already visited: " + url);
            return;
        }

        try {
            // Fetch the document using JSoup
            Document doc = Jsoup.connect(url)
                                .userAgent("Mozilla/5.0 (compatible; Downloader/1.0)")
                                .timeout(5000)
                                .get();

            // Print the page title
            System.out.println("Title: " + doc.title());

            // Mark URL as visited
            visitedUrls.add(url);

            // Extract and print all absolute links on the page
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                if (!nextUrl.isEmpty() && !visitedUrls.contains(nextUrl)) {
                    System.out.println("Found link: " + nextUrl);
                    // Optionally, you could add this URL to a queue for further processing
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching URL: " + url + " - " + e.getMessage());
        }
    }
}
