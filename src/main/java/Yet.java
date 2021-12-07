import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Yet {

    private static Set linksToVisit = Collections.synchronizedSet(new HashSet());
    private static Set linksVisited = Collections.synchronizedSet(new HashSet());
    private static Set emailSet = Collections.synchronizedSet(new HashSet());
    private static Map domainMap = Collections.synchronizedMap(new HashMap<>());
    private static Set blackList = Collections.synchronizedSet(new HashSet());
    private static final int EMAIL_MAX = 10_000;
    private static int x = 15000;
    private static final Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_.+\\-]+@[a-zA-Z0-9\\-]+\\.[a-z]+");
    //"(?:mailto:)([a-zA-Z1-9\\.]+@[a-zA-Z1-9]+\\.[a-zA-Z1-9]+)"


    public static void main(String[] args) throws IOException, NoSuchElementException, InterruptedException, ConcurrentModificationException {
        
        ExecutorService pool = Executors.newFixedThreadPool(500);

    }

    static class Scraper implements Runnable {

        private final String linkToCheck;
        private Set extractedLinks = new HashSet();
        private final Set extractedEmails = new HashSet();

        public Scraper(String link) {
            this.linkToCheck = link;
        }

        @Override
        public void run() {

        }


    }
}





