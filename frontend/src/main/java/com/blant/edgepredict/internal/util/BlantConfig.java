package com.blant.edgepredict.internal.util;

public class BlantConfig {
   public static String BLANT_URL = "http://localhost:49161";
   public static String SUBMIT_URL;
   public static String RESULTS_URL;
   public static String PROGRESS_URL;
   private static String JOB_ID;
   private static int progress;
   private static boolean isLoad;

   public static void setJobId(String r) {
      JOB_ID = r;
   }

   public static String getJobId() {
      return JOB_ID;
   }

   public static void setProgress(int p) {
      progress = p;
   }

   public static int getProgress() {
      return progress;
   }

   public static void setLoad(boolean load) {
      isLoad = load;
   }

   public static boolean getLoad() {
      return isLoad;
   }

   static {
      SUBMIT_URL = BLANT_URL + "/blant";
      RESULTS_URL = BLANT_URL + "/results/";
      PROGRESS_URL = BLANT_URL + "/progress/";
      JOB_ID = null;
      progress = 0;
      isLoad = false;
   }
}
