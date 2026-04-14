package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class QuoteService {

    private static final String ZEN_URL = "https://zenquotes.io/api/random";

    public String fetchRandomQuote() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(ZEN_URL, String.class);
            int start = response.indexOf("\"q\":\"") + 5;
            int end = response.indexOf("\"", start);
            return response.substring(start, end);
        } catch (Exception e) {
            System.err.println("Failure to fetch quote:" + e.getMessage());
            return null;
        }
    }
}