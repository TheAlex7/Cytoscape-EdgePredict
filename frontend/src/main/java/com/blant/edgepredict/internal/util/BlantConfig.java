package com.blant.edgepredict.internal.util;
import java.net.URL;
import java.net.HttpURLConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;


// Utility class to hold configuration for BLANT server and job ID
// Only contains 1 job ID at a time, since the plugin is designed to run one job at a time
public class BlantConfig {

    // Root URL for the BLANT server
    public static String BLANT_URL = "http://localhost:55161";

    // URL for submitting jobs
    public static String SUBMIT_URL = BLANT_URL + "/blant";

    // URL for getting results
    public static String RESULTS_URL = BLANT_URL + "/results/";

    // URL for progress updates
    public static String PROGRESS_URL = BLANT_URL + "/progress/";


    private static String JOB_ID = null;
    
    public static void setJobId(String r) {
        JOB_ID = r;
    }
    public static String getJobId() {
        return JOB_ID;
    }
}
