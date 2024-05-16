package org.fotum.app.config;

import org.fotum.app.MainApp;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Config extends JSONObject {
    private static Config instance;

    private Config(InputStream inStream) {
        super(new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n")));
    }

    public static Config getInstance() {
        if (instance == null) {
            synchronized(Config.class) {
                if (instance == null) {
                    try {
                        InputStream configRes = MainApp.class.getResourceAsStream("/botconfig.json");
                        instance = new Config(configRes);
                        configRes.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        return instance;
    }
}
