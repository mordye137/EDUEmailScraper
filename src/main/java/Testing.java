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


public class Testing {

    private static Set linksToVisit = Collections.synchronizedSet(new HashSet());
    private static Set linksVisited = Collections.synchronizedSet(new HashSet());
    private static Set emailSet = Collections.synchronizedSet(new HashSet());
    private static Map domainMap = Collections.synchronizedMap(new HashMap<>());
    private static Set blackList = Collections.synchronizedSet(new HashSet());
    private static final int emailMax = 10000;
    private static int x = 15000;
    private static final Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_.+\\-]+@[a-zA-Z0-9\\-]+\\.[a-z]+");
    //"(?:mailto:)([a-zA-Z1-9\\.]+@[a-zA-Z1-9]+\\.[a-zA-Z1-9]+)"


    public static void main(String[] args) throws IOException, NoSuchElementException, InterruptedException, ConcurrentModificationException {

        ExecutorService pool = Executors.newFixedThreadPool(500);

        //Add starting link to the set
        linksToVisit.add("https://touro.edu");

        //Add pre-determined blacklisted sites
        blackList.add("tumblr");
        blackList.add("signin");
        blackList.add("jpg");
        blackList.add("jpeg");
        blackList.add("flickr");
        blackList.add("wikipedia");
        blackList.add("med.psu.edu");
        blackList.add("pubs.acs.org");
        blackList.add("www.congress.gov ");
        blackList.add("data.cdc.gov");
        blackList.add("www.pgatoursuperstore.com");

        try {
        while (emailSet.size() <= emailMax) {

            //Get the first link from the set
            String urlToCHeck = linksToVisit
                    .stream()
                    .findFirst()
                    .get()
                    .toString();

                if (!domainMapperAndCheck(urlToCHeck)) {
                    pool.execute(new Scraper(urlToCHeck));
                    Thread.sleep(x);
                    x = 1000;

                    System.out.println("Checking " + urlToCHeck);
                } else {
                    setRemover(linksToVisit, urlToCHeck);
                }
        }

        pool.shutdown();

        System.out.println(linksVisited.size());
        System.out.println(emailSet.size());

        } catch (ConcurrentModificationException e) {
            System.out.println(e);
        }
     }

     static class Scraper implements Runnable{

        private final String linkToCheck;
        private Set extractedLinks = new HashSet();
        private final Set extractedEmails = new HashSet();

        public Scraper(String link){
            this.linkToCheck = link;
        }

        @Override
        public void run() {

            try {
                linksVisited.add(linkToCheck);
                setRemover(linksToVisit, linkToCheck);

                Document doc = Jsoup.connect(linkToCheck)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(30000)
                        .get();

                //Gets all links from the url and adds them to internal set
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String foundlink = link.absUrl("href");
                    if (!foundlink.contains("mailto") && (!linksToVisit.contains(foundlink)))
                        extractedLinks.add(foundlink);
                }

                //Regex to find email links
                Matcher matcher = emailPattern.matcher(doc.text());

                //Adds all emails found to email set
                while (matcher.find()){
                    if (!emailSet.contains(matcher.group())){
                        extractedEmails.add(matcher.group().toLowerCase());
                    }

                }

                extractedLinks.removeAll(linksVisited);
                linkRemover(blackList, extractedLinks);
                linksToVisit.addAll(extractedLinks);

                emailSet.addAll(extractedEmails);

                System.out.println("Emails- " + emailSet.size());
                System.out.println("Links- " + linksToVisit.size());

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    //Method to remove blacklisted links from the extracted links
    private static synchronized Set linkRemover(Set<String> blacklist, Set<String> linkSet) {
         Iterator linkSetIterator = linkSet.iterator();

         while (linkSetIterator.hasNext()){
             String linkCheck = (String) linkSetIterator.next();
             if (blacklist.stream().anyMatch(linkCheck::contains))
                 linkSetIterator.remove();
         }
         return linkSet;
    }

    //Method to map the visited domains and find out if the domain we
    //are trying to visit is blacklisted or not
    private static synchronized boolean domainMapperAndCheck(String urlToCHeck){
         try{
            URL url = new URL(urlToCHeck);
            String domain = url.getHost();

            if (domainMap.containsKey(domain)) {
                if ((int) domainMap.get(domain) >= 100) {
                    System.out.println(domain + " maxed out");
                    setRemover(linksToVisit, urlToCHeck);
                    blackList.add(domain);
                }
                domainMap.put(domain, (int) domainMap.get(domain) + 1);
            } else {
                domainMap.put(domain, 1);
            }
            return (blackList.contains(domain));

        } catch (MalformedURLException e){
             setRemover(linksToVisit, urlToCHeck);
        }
        return true;
     }

     //Method to easily remove items from a Set using an iterator
    private static synchronized void setRemover(Set set, String toRemove){
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String check = iterator.next();
            if (check.equals(toRemove)) {
                iterator.remove();
            }
        }
    }
}






