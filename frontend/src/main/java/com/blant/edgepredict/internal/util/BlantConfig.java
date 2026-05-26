package com.blant.edgepredict.internal.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BlantConfig {
    public static boolean isOnline = true;
    private static final AtomicBoolean isAborted = new AtomicBoolean(false);

    public static final String BLANT_URL = "http://bayonet-10.ics.uci.edu:49161/";
    public static final String BLANT_URL_LOCAL = "http://localhost:49161";

    private static final AtomicReference<String> JOB_ID = new AtomicReference<>(null);
    private static final AtomicInteger progress = new AtomicInteger(0);
    private static final AtomicBoolean isLoad = new AtomicBoolean(false);
    private static final AtomicBoolean isForce = new AtomicBoolean(false);
    private static final AtomicReference<java.io.File> INPUT_FILE = new AtomicReference<>(null);

    private BlantConfig() {
    }

    public static void setJobId(String r) {
        JOB_ID.set(r);
    }

    public static String getJobId() {
        return JOB_ID.get();
    }

    public static void setProgress(int p) {
        progress.set(p);
    }

    public static int getProgress() {
        return progress.get();
    }

    public static void setLoad(boolean load) {
        isLoad.set(load);
    }

    public static boolean getLoad() {
        return isLoad.get();
    }

    public static void setForce(boolean force) {
        isForce.set(force);
    }

    public static boolean getForce() {
        return isForce.get();
    }

    public static void setInputFile(java.io.File f) {
        INPUT_FILE.set(f);
    }

    public static java.io.File getInputFile() {
        return INPUT_FILE.get();
    }

    public static void setOnline(boolean online) {
        isOnline = online;
    }

    public static boolean getOnline() {
        return isOnline;
    }

    public static void setAborted(boolean aborted) {
        isAborted.set(aborted);
    }

    public static boolean getAborted() {
        return isAborted.get();
    }

    public static String getSubmitUrl() {
        if (isOnline) return BLANT_URL + "/blant";
        else return BLANT_URL_LOCAL + "/blant";
    }

    public static String getResultUrl() {
        if (isOnline) return BLANT_URL + "/results/";
        else return BLANT_URL_LOCAL + "/results/";
    }

    public static String getProgressUrl() {
        if (isOnline) return BLANT_URL + "/progress/";
        else return BLANT_URL_LOCAL + "/progress/";
    }

    public static String getAbortUrl() {
        if (isOnline) return BLANT_URL + "/abort/" + BlantConfig.getJobId();
        else return BLANT_URL_LOCAL + "/abort/" + BlantConfig.getJobId();
    }
}
