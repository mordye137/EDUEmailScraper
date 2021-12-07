import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper1 {
    public static void main(String[] args) throws IOException {

        Set linksToVisitSet = Collections.synchronizedSet(new HashSet());
        Set emailSet = Collections.synchronizedSet(new HashSet());
        Set linksVisited = new HashSet();
        int emailMax = 5;

        linksToVisitSet.add("https://touro.edu");
//        URL url = new URL("https://touro.edu");
//        Scanner sc = new Scanner(url.openStream());
//        StringBuffer sb = new StringBuffer();
//        while (sc.hasNext()){
//            sb.append(sc.next());
//        }
//        Pattern emailPattern = Pattern.compile("([a-zA-Z1-9\\.]+@[a-zA-Z1-9]+\\.[a-zA-Z1-9]+)");
//        Matcher matcher = emailPattern.matcher(sb);
//        Pattern linkPattern = Pattern.compile("https?:\\/\\/((?:[\\w\\d-]+\\.)+[\\w\\d]{2,})");
//        Matcher matcher1 = linkPattern.matcher(sb);
//
//        while (matcher.find()){
//            emailSet.add(matcher.group(1));
//        }
//        while (matcher1.find()){
//            linkSet.add(matcher1.group());
//        }
//        System.out.println(emails);
//        System.out.println(linkSet);

        while (! linksToVisitSet.isEmpty() && emailSet.size() <= emailMax){
                Set extractedLinks = new HashSet();
                String link = linksToVisitSet.stream().findFirst().get().toString();
                System.out.println(link);
                URL url = new URL(link);
                try {
                    Scanner sc = new Scanner(url.openStream());
                    StringBuffer sb = new StringBuffer();
                    while (sc.hasNext()) {
                        sb.append(sc.next());
                    }

                    Pattern linkPattern = Pattern.compile("https?:\\/\\/((?:[\\w\\d-]+\\.)+[\\w\\d]{2,})");
                    Matcher linkMatcher = linkPattern.matcher(sb);
                    Pattern emailPattern = Pattern.compile("(?:mailto:)([a-zA-Z1-9\\.]+@[a-zA-Z1-9]+\\.[a-zA-Z1-9]+)");
                    Matcher matcher = emailPattern.matcher(sb);

                    while (linkMatcher.find()) {
                        extractedLinks.add(linkMatcher.group());
                    }
                    while (matcher.find()){
                        emailSet.add(matcher.group(1));
                    }
                } catch (IOException e) {
                    System.out.println("error");
                    linksVisited.add(link);
                    linksToVisitSet.remove(link);
                    continue;
                }
                linksVisited.add(link);
                linksToVisitSet.remove(link);
                extractedLinks.removeAll(linksVisited);
                linksToVisitSet.addAll(extractedLinks);
                System.out.println("Got links");
            }
        System.out.println(emailSet);
        System.out.println(emailSet.size());
        }

}


