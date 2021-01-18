package com.example.edubot;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.stream.Collectors;

public class FetchWiki {
    private static final String encoding = "UTF-8";

    private String WikiSearch;


    public String QueryWiki(String wikiSearch) throws IOException {
        final String encoding = "UTF-8";

        //Wait for user response
        //System.out.println("\n\nType something that you want me to search on the internet...");
        //String nextLine = scanner.nextLine();
        //String searchText = wikiSearch + " wikipedia";
        //System.out.println("Searching on the web....");


        //Get the first link about Wikipedia
        String wikipediaURL = "https://bn.wikipedia.org/wiki/"+wikiSearch;

        //Use Wikipedia API to get JSON File
        String wikipediaApiJSON = "https://bn.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles="
                + URLEncoder.encode(wikipediaURL.substring(wikipediaURL.lastIndexOf("/") + 1, wikipediaURL.length()), encoding);

        //Let's see what it found
        //System.out.println(wikipediaURL);
        //System.out.println(wikipediaApiJSON);

        //"extract":" the summary of the article
        HttpURLConnection httpcon = (HttpURLConnection) new URL(wikipediaApiJSON).openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));

        //Read line by line
        String responseSB = in.lines().collect(Collectors.joining());
        in.close();

        //Print the result for us to see
        //System.out.println(responseSB);
        String result = responseSB.split("extract\":\"")[1];
        //System.out.println(result);

        //Tell only the 150 first characters of the result
        String textToTell = result.length() > 1000 ? result.substring(0, 100) : result;

        String output= StringEscapeUtils.unescapeJava(textToTell);

        return output;

    }
}
