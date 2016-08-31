package io.netty.websocket;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by alfasens on 16.05.16.
 */
public class WebSocketServerConfiguration {

    private Map<String, String> dictionary;

    public WebSocketServerConfiguration(String filename) {
        dictionary = new HashMap<>();
        readConfigurationFile(filename);
    }

    public String getValueOf(String parameter) {
        return dictionary.get(parameter);
    }

    private void readConfigurationFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split("=");
                dictionary.put(data[0].trim(), data[1].trim());
            }
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
    }

}
