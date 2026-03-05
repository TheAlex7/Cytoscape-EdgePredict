package com.blant.edgepredict.internal.util;
import java.net.URL;
import java.net.HttpURLConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;


public class BlantConfig {

    public static String BLANT_URL = "http://localhost:55161";

    private static String result;

    public static void setJobId(String r) {
        result = r;
    }

    public static String getJobId() {
        return result;
    }
}
