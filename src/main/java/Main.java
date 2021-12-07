import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {

    Set linksToVisit = Collections.synchronizedSet(new HashSet());
    Set linksVisited = Collections.synchronizedSet(new HashSet());
    Set emailSet = Collections.synchronizedSet(new HashSet());
    Map domainMap = Collections.synchronizedMap(new HashMap<>());
    Set blackList = Collections.synchronizedSet(new HashSet());
    final int EMAIL_MAX = 5_000;
    int x = 15000;
    final Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_.+\\-]+@[a-zA-Z0-9\\-]+\\.[a-z]+");


    public void Scrape() throws IOException, NoSuchElementException, InterruptedException, ConcurrentModificationException {

        ExecutorService pool = Executors.newFixedThreadPool(2);

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
            synchronized (this) {
                while (emailSet.size() <= EMAIL_MAX) {

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
                dbUpload(emailSet);
            }
        } catch (ConcurrentModificationException | ClassNotFoundException e) {
            System.out.println(e.getCause());
        }
    }

    private class Scraper implements Runnable {

        private String linkToCheck;
        private Set extractedLinks = new HashSet();
        private Set extractedEmails = new HashSet();

        public Scraper(String link) {
            this.linkToCheck = link;
        }

        @Override
        public void run() {

            try {
                synchronized (this) {
                linksVisited.add(linkToCheck);
                setRemover(linksToVisit, linkToCheck);

                Document doc = Jsoup.connect(linkToCheck)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(30000)
                        .get();

                //Gets all links from the url and adds them to internal set
                Elements links = doc.getElementsByTag("a");

                for (Element link : links) {

                    if (!link.attr("abs:href").contains("mailto"))
                        extractedLinks.add(link.attr("abs:href"));

                    if (link.attr("abs:href").contains("mailto")) {
                        extractedEmails.add(link.attr("abs:href").substring(7));
                    }
                }

                //Regex to find email links
                Matcher matcher = emailPattern.matcher(doc.body().html());

                //Adds all emails found to email set

                    while (matcher.find()) {
                        emailSet.add(matcher.group());
                    }


                    extractedLinks.removeAll(linksVisited);
                    linkRemover(blackList, extractedLinks);
                    linksToVisit.addAll(extractedLinks);

                    System.out.println(extractedEmails);

                    System.out.println("Emails- " + emailSet.size());
                    System.out.println("Links- " + linksToVisit.size());
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    //Method to remove blacklisted links from the extracted links
    public synchronized Set linkRemover(Set<String> blacklist, Set<String> linkSet) {
        Iterator linkSetIterator = linkSet.iterator();

        while (linkSetIterator.hasNext()) {
            String linkCheck = (String) linkSetIterator.next();
            System.out.println(linkCheck);
            if (blacklist.stream().anyMatch(linkCheck::contains))
                System.out.println("removing");
                linkSetIterator.remove();
        }
        return linkSet;
    }

    //Method to map the visited domains and find out if the domain we
    //are trying to visit is blacklisted or not
    public synchronized boolean domainMapperAndCheck(String urlToCHeck) {
        try {
            URL url = new URL(urlToCHeck);
            String domain = url.getHost();

            if (domainMap.containsKey(domain)) {
                if ((int) domainMap.get(domain) >= 50) {
                    blackList.add(domain);
                    System.out.println(domain + " maxed out");
                    setRemover(linksToVisit, urlToCHeck);
                    linkRemover(blackList, linksToVisit);
                }
                domainMap.put(domain, (int) domainMap.get(domain) + 1);
            } else {
                domainMap.put(domain, 1);
            }
            return (blackList.contains(domain));

        } catch (MalformedURLException e) {
            setRemover(linksToVisit, urlToCHeck);
        }
        return true;
    }

    //Method to easily remove items from a Set using an iterator
    public synchronized void setRemover(Set set, String toRemove) {
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String check = iterator.next();
            if (check.equals(toRemove)) {
                iterator.remove();
            }
        }
    }

    //Method that adds the email set to the database
    public void dbUpload(Set emailSet) throws ClassNotFoundException {
        int counter = 0;

        String connectionUrl = // specifies how to connect to the database
                "jdbc:sqlserver://mco364.ckxf3a0k0vuw.us-east-1.rds.amazonaws.com;"
                        + "database=MordyEpstein;"
                        + "user=admin364;"
                        + "password=mco364lcm;"
                        + "encrypt=false;"
                        + "trustServerCertificate=false;"
                        + "loginTimeout=30;";
        ResultSet resultSet = null;

        String statement = "INSERT INTO EMAILS (Email) VALUES (?);";

        try (Connection conn = DriverManager.getConnection(connectionUrl)){
            PreparedStatement preparedStatement = conn.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
            for (Object email: emailSet) {
                String emailString = email.toString();
                preparedStatement.setString(1, emailString);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}

class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        Main scraper = new Main();
        scraper.Scrape();
    }
}






