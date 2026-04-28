package com.blant.edgepredict.internal.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BlantConfig {
    public static final String BLANT_URL = "http://localhost:49161";
    public static final String SUBMIT_URL = BLANT_URL + "/blant";
    public static final String RESULTS_URL = BLANT_URL + "/results/";
    public static final String PROGRESS_URL = BLANT_URL + "/progress/";

    private static final AtomicReference<String> JOB_ID = new AtomicReference<>(null);
    private static final AtomicInteger progress = new AtomicInteger(0);
    private static final AtomicBoolean isLoad = new AtomicBoolean(false);
    private static final AtomicReference<java.io.File> INPUT_FILE = new AtomicReference<>(null);

    private BlantConfig() {}

    public static void setJobId(String r) { JOB_ID.set(r); }
    public static String getJobId() { return JOB_ID.get(); }

    public static void setProgress(int p) { progress.set(p); }
    public static int getProgress() { return progress.get(); }

    public static void setLoad(boolean load) { isLoad.set(load); }
    public static boolean getLoad() { return isLoad.get(); }

    public static void setInputFile(java.io.File f) { INPUT_FILE.set(f); }
    public static java.io.File getInputFile() { return INPUT_FILE.get(); }
}
