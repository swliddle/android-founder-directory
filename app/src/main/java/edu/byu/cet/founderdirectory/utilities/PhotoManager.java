package edu.byu.cet.founderdirectory.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class to manage photos associated with Founder records.
 *
 * Created by Liddle on 3/21/16.
 */
public class PhotoManager {

    /**
     * Tag for logging.
     */
    private static final String TAG = "PhotoManager";

    /**
     * Reference to singleton PhotoManager.
     */
    private static PhotoManager sSharedInstance;

    /**
     * Context for performing file operations.
     */
    private Context mContext;

    /**
     * Private constructor for building a PhotoManager.
     *
     * @param context Context for performing file operations
     */
    private PhotoManager(Context context) {
        mContext = context;
    }

    /**
     * Accessor to retrieve the shared PhotoManager singleton instance.
     *
     * @param context Context for performing file operations
     * @return PhotoManager singleton
     */
    public static @NonNull PhotoManager getSharedPhotoManager(Context context) {
        if (sSharedInstance == null) {
            sSharedInstance = new PhotoManager(context);
        }

        return sSharedInstance;
    }

    /**
     * Return the bitmap for a photo corresponding to a given filename.
     *
     * @param filename A filename
     * @return The corresponding photo bitmap
     */
    public Bitmap getPhoto(String filename) {
        File photoFile = fileForExistingPhotoUrl(filename);

        if (photoFile != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
        }

        return null;
    }

    /**
     * Get a photo for a given Founder record ID.
     *
     * @param id A Founder record ID
     * @return The corresponding photo bitmap
     */
    public Bitmap getPhotoForFounderId(int id) {
        return getPhoto("founder" + id);
    }

    /**
     * Get a spouse photo for a given Founder record ID.
     *
     * @param id A Founder record ID
     * @return The corresponding spouse photo bitmap
     */
    public Bitmap getSpousePhotoForFounderId(int id) {
        return getPhoto("spouse" + id);
    }

    /**
     * Save a photo for a given Founder record ID.
     *
     * @param id A Founder record ID
     * @param photo The bitmap to save as the Founder photo
     */
    public void savePhotoForFounderId(int id, Bitmap photo) {
        savePhoto("founder" + id, photo);
    }

    /**
     * Save a spouse photo for a given Founder record ID.
     *
     * @param id A Founder record ID
     * @param photo The bitmap to save as the Founder spouse photo
     */
    public void saveSpousePhotoForFounderId(int id, Bitmap photo) {
        savePhoto("spouse" + id, photo);
    }

    /**
     * Get the full URL string for a given image filename.
     *
     * @param imageFileName The name of an image (e.g. founder13)
     * @return If the image file exists, its full path
     */
    public String urlForFileName(String imageFileName) {
        File photoFile = fileForExistingPhotoUrl(imageFileName);

        if (photoFile != null) {
            return photoFile.getAbsolutePath();
        }

        return null;
    }

    /**
     * Retrieve a File corresponding
     * @param url
     * @return
     */
    private File fileForExistingPhotoUrl(String url) {
        File[] cacheDirs = ContextCompat.getExternalCacheDirs(mContext);

        for (File dir : cacheDirs) {
            File photoFile = new File(dir.getAbsolutePath() + File.separator + url);

            if (photoFile.exists()) {
                return photoFile;
            }
        }

        return null;
    }

    private File fileForNewPhotoUrl(String url) {
        File cacheDir = ContextCompat.getExternalCacheDirs(mContext)[0];

        return new File(cacheDir.getAbsolutePath() + File.separator + url);
    }

    private void savePhoto(String url, Bitmap photo) {
        File photoFile = fileForNewPhotoUrl(url);

        if (photoFile != null) {
            if (photoFile.exists()) {
                photoFile.delete();
            }

            FileOutputStream out = null;

            try {
                out = new FileOutputStream(photoFile);
                photo.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "savePhoto unable to save: " + e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "savePhoto unable to close: " + e);
                }
            }
        }
    }
}