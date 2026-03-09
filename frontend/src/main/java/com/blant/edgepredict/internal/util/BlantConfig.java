package com.blant.edgepredict.internal.util;
import java.net.URL;
import java.net.HttpURLConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;


// Utility class to hold configuration for BLANT server and job ID
// Only contains 1 job ID at a time, since the plugin is designed to run one job at a time
// ISSUE:
// -------JSION PARSING-------
// Currently the parsing in json is very brittle and assumes a specific format. 
// This is fine for now since we control the server, but if the server response format changes this will break. 
// A more robust solution would be to use a JSON parsing library like Jackson or Gson, 
// but that would add an additional dependency to the project. 
// For now, we will keep it simple and just document the expected format in the server response.
// DEPENDENCIES:
// Currently affects any code that reads json back from the server which is SendToBlant and BlantPoller


public class BlantConfig {

    // Root URL for the BLANT server
    public static String BLANT_URL = "http://localhost:55161";

    // URL for submitting jobs
    public static String SUBMIT_URL = BLANT_URL + "/blant";

    // URL for getting results
    public static String RESULTS_URL = BLANT_URL + "/results/";

    // URL for progress updates
    public static String PROGRESS_URL = BLANT_URL + "/progress/";

    //Job ID management
    private static String JOB_ID = null;
    public static void setJobId(String r) {
        JOB_ID = r;
    }
    public static String getJobId() {
        return JOB_ID;
    }
}
