package edu.byu.cet.founderdirectory.utilities;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

import edu.byu.cet.founderdirectory.service.SyncService;

/**
 * Class that implements DIY and Flurry analytics.
 */
public class AnalyticsManager {
    /**
     * Tag for logging.
     */
    private static final String TAG = "AnalyticsManager";

    /**
     * Default URL encoding schema.
     */
    public static String DEFAULT_ENCODING = "UTF-8";

    /**
     * Script that can handle the server-side tracking of a usage report.
     */
    private static final String REPORT_USAGE = "r.php";
    /*
     * Note: this r.php endpoint should simply dump parameters into a database
     * table, perhaps like this PHP script:

    <?php
        require_once("/var/uniklu-inc/opendb.inc");

        $d = addslashes($_REQUEST['d']);
        $m = addslashes($_REQUEST['m']);
        $o = addslashes($_REQUEST['o']);
        $p = addslashes($_REQUEST['p']);
        $r = addslashes($_REQUEST['r']);
        $s = addslashes($_REQUEST['s']);
        $g = addslashes($_REQUEST['g']);
        $v = addslashes($_REQUEST['v']);
        $u = addslashes($_REQUEST['u']);

        $query = "INSERT INTO stats (device,manufacturer,model,product,rel,sdk,page,version,url) " .
                 "VALUES ('$d','$m','$o','$p','$r','$s','$g','$v','$u')";
        $db->query($query);

        exit(0);
    ?>
     */

    /**
     * SDK vjava.lang.Stringersion number.
     */
    private static final int sSdkVersion = Build.VERSION.SDK_INT;

    /**
     * Reference to singleton instance.
     */
    private static AnalyticsManager sAnalyticsManager = null;

    /**
     * Version number for this app.
     */
    private static String sAppVersion = null;

    /**
     * Hashed device ID for this device.
     */
    private static String sDeviceIdentity = null;

    /**
     * Private constructor for the singleton analytics manager.
     */
    private AnalyticsManager(Application app) {
        // Initialize app version and device identity.
        PackageInfo info = null;
        try {
            info = app.getPackageManager().getPackageInfo("at.ac.uniklu.mobile", 0);
        } catch (NameNotFoundException e) {
            // Ignore.
        }

        if (info != null) {
            sAppVersion = info.versionName;
        } else {
            sAppVersion = "0.1";
        }

        sDeviceIdentity = Utilities.getDeviceId(app);
    }

    public static Map<String, String> getDeviceParameters(String pageUrl) {
        Map<String, String> params = new HashMap<String, String>();

        params.put("d", sDeviceIdentity);
        params.put("m", Build.BRAND);
        params.put("o", Build.MODEL);
        params.put("p", Build.PRODUCT);
        params.put("r", Build.VERSION.RELEASE);
        params.put("s", sSdkVersion + "");
        params.put("v", sAppVersion);
        params.put("u", pageUrl);

        return params;
    }

    /**
     * Get singleton instance suitable for the installed version of Android.
     *
     * @return Singleton instance.
     */
    public static AnalyticsManager getInstance(Application app) {
        if (sAnalyticsManager == null) {
            if (sSdkVersion < Build.VERSION_CODES.DONUT) {
                sAnalyticsManager = new AnalyticsManager(app);
            } else {
                sAnalyticsManager = new DonutAnalyticsManager(app);
            }
        }

        return sAnalyticsManager;
    }

    /**
     * Report a page load.
     *
     * @param pageName Name of the page loaded.
     * @param pageUrl  URL information, or empty string.
     */
    public void report(String pageName, String pageUrl) {
        try {
            Map<String, String> params = AnalyticsManager.getDeviceParameters(pageUrl);

            // DIY analytics.
            params.put("g", pageName);
            reportUsage(params);
        } catch (Exception e) {
            // Ignore.
        }
    }

    /**
     * Tell our server about usage statistics. Do this asynchronously so we don't hang up the application if there's a
     * problem.
     * <p/>
     * NEEDSWORK: do this through a queue of some sort instead.
     *
     * @param params
     */
    public static void reportUsage(Map<String, String> params) {
        try {
            boolean first = true;
            StringBuffer encodedUrl = new StringBuffer(SyncService.SYNC_SERVER_URL);

            encodedUrl.append(REPORT_USAGE);

            for (String key : params.keySet()) {
                if (first) {
                    first = false;
                    encodedUrl.append("?");
                } else {
                    encodedUrl.append("&");
                }
                encodedUrl.append(key);
                encodedUrl.append("=");
                encodedUrl.append(URLEncoder.encode(params.get(key), DEFAULT_ENCODING));
            }

            HttpHelper.sendAsyncHttpMessage(encodedUrl.toString());
        } catch (Exception e) {
            Log.d(TAG, "reportUsage: " + e);
        }
    }

    /**
     * Class that implements DIY analytics.
     */
    private static class DonutAnalyticsManager extends AnalyticsManager {
        private DonutAnalyticsManager(Application app) {
            super(app);
        }

        /**
         * Report a page load.
         *
         * @param pageName Name of the page loaded.
         * @param pageUrl  URL information, or empty string.
         */
        @Override
        public void report(String pageName, String pageUrl) {
            try {
                Map<String, String> params = AnalyticsManager.getDeviceParameters(pageUrl);

                // DIY analytics.
                params.put("g", pageName);
                reportUsage(params);
            } catch (Exception e) {
                // Ignore.
            }

            try {
                // Flurry analytics.
            } catch (Exception e) {
                // Ignore.
            }
        }
    }
}