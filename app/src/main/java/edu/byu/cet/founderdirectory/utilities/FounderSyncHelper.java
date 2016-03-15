package edu.byu.cet.founderdirectory.utilities;

import android.util.Log;

import edu.byu.cet.founderdirectory.provider.FounderProvider;

/**
 * Created by Liddle on 3/15/16.
 */
public class FounderSyncHelper {

    private static final String TAG = "FounderSyncHelper";

    private static FounderSyncHelper sSyncHelper;
    private static int counter = 0;

    private FounderSyncHelper() {

    }

    public static FounderSyncHelper getInstance() {
        if (sSyncHelper == null) {
            sSyncHelper = new FounderSyncHelper();
        }

        return sSyncHelper;
    }

    public void syncFounders() {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
//                synchronized (sSyncHelper) {
                    Log.d(TAG, "run: instance " + counter++ + " hits the wall");
//                }
            }
        });

        worker.start();
    }
}