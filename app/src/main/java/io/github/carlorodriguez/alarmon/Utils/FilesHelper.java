package io.github.carlorodriguez.alarmon.Utils;


import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.loader.content.CursorLoader;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.github.carlorodriguez.alarmon.R;

/**
 * Camera related utilities.
 */
public class FilesHelper {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_Audio = 3;

    private static String TAG = FilesHelper.class.getSimpleName();

    /**
     * Iterate over supported camera video sizes to see which one best fits the
     * dimensions of the given view while maintaining the aspect ratio. If none can,
     * be lenient with the aspect ratio.
     *
     * @param supportedVideoSizes Supported camera video sizes.
     * @param previewSizes Supported camera preview sizes.
     * @param w     The width of the view.
     * @param h     The height of the view.
     * @return Best match camera video size to fit in the view.
     */
    public static Camera.Size getOptimalVideoSize(List<Camera.Size> supportedVideoSizes,
                                                  List<Camera.Size> previewSizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        List<Camera.Size> videoSizes;
        if (supportedVideoSizes != null) {
            videoSizes = supportedVideoSizes;
        } else {
            videoSizes = previewSizes;
        }
        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = h;

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * @return the default camera on the device. Return null if there is no camera on the device.
     */
    public static Camera getDefaultCameraInstance() {
        return Camera.open();
    }


    /**
     * @return the default rear/back facing camera on the device. Returns null if camera is not
     * available.
     */
    public static Camera getDefaultBackFacingCameraInstance() {
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * @return the default front facing camera on the device. Returns null if camera is not
     * available.
     */
    public static Camera getDefaultFrontFacingCameraInstance() {
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }


    /**
     *
     * @param position Physical position of the camera i.e Camera.CameraInfo.CAMERA_FACING_FRONT
     *                 or Camera.CameraInfo.CAMERA_FACING_BACK.
     * @return the default camera on the device. Returns null if camera is not available.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static Camera getDefaultCamera(int position) {
        // Find the total number of cameras available
        int  mNumberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the back-facing ("default") camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == position) {
                return Camera.open(i);

            }
        }

        return null;
    }

    /**
     * Creates a media file in the {@code Environment.DIRECTORY_PICTURES} directory. The directory
     * is persistent and available to other applications like gallery.
     *
     * @param type Media type. Can be video or image.
     * @return A file object pointing to the newly created file.
     */
    public static File getOutputMediaFile(Context context, int type){
        Log.d(TAG, "getOutputMediaFile started");

        File mediaStorageDir = null;
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            Log.d(TAG, "getExternalStorageState not exist");
            // If files not meant to be shared with other apps, store them in your package-specific directories
            // Files will be deleted after your app has been uninstalled.
            mediaStorageDir =  new File(context.getFilesDir(), Environment.DIRECTORY_ALARMS);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //mediaStorageDir = new File(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + File.separator + Environment.DIRECTORY_ALARMS);
                // If files not meant to be shared with other apps, store them in your package-specific directories
                // Files will be deleted after your app has been uninstalled.
                mediaStorageDir =  new File(context.getExternalFilesDir(Environment.DIRECTORY_ALARMS).getAbsolutePath());
            }else{
                // This location works best if you want the created media to be shared
                // between applications and persist after your app has been uninstalled.
                mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
            }
        }

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");

        }else if(type == MEDIA_TYPE_Audio) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "KissAlarm_"+ timeStamp + ".wav");
        }else{
            return null;
        }

        Log.d(TAG, "mediaFile = "+ mediaFile.getAbsolutePath());
        return mediaFile;
    }

    /**
     * A function to determine mime type of file
     *
     * @param uri is the uri of the file we need to check.
     * @param context is needed for the resolver
     * @mimeType A string of the mimeType
     */
    public static String getMimeTyp(Uri uri, Context context) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver resolver = context.getContentResolver();
            mimeType = resolver.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (fileExtension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
            }
        }
        return mimeType;
    }

    /**
     * A function to get a filename form Uri
     *
     * @param uri is the uri of the file we need to check.
     * @param context is needed for the resolver
     * @filename A string of the filename
     */
    public static String getFileName(Uri uri , Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            // The query, because it only applies to a single document, returns only
            // one row. There's no need to filter, sort, or select fields,
            // because we want all fields for one document.
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                // moveToFirst() returns false if the cursor has 0 rows. Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (cursor != null && cursor.moveToFirst()) {
                    // Note it's called "Display Name". This is
                    // provider-specific, and might not necessarily be the file name.
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getRealPathFromURI(Uri uri , Context context) {
        String  result = null;
        String[] proj;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            proj = null;
        }else{
            proj = new String[]{MediaStore.Audio.Media.DATA};
        }

        Log.d(TAG, "getFileRealPath: scheme= "+uri.getScheme());
        if (uri.getScheme().equals("content")) {
            Log.d(TAG, "getFileRealPath: it's a content scheme");
            // The query, because it only applies to a single document, returns only
            // one row. There's no need to filter, sort, or select fields,
            // because we want all fields for one document.
            //Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
            CursorLoader loader = new CursorLoader(context, uri, proj, null, null, null);
            Cursor cursor = loader.loadInBackground();
            try {
                // moveToFirst() returns false if the cursor has 0 rows. Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (cursor != null && cursor.moveToFirst()) {
                    //result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    //result = Uri.parse(cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_MEDIA_URL)));
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        result= cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH));
                    }else{
                        result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    }

                    // Note it's called "Display Name". This is
                    // provider-specific, and might not necessarily be the file name.
                    /*String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    Log.i(TAG, "Display Name: " + displayName);
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                    int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));*/

                    /*int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    // If the size is unknown, the value stored is null. But because an
                    // int can't be null, the behavior is implementation-specific,
                    // and unpredictable. So as
                    // a rule, check if it's null before assigning to an int. This will
                    // happen often: The storage API allows for remote files, whose
                    // size might not be locally known.
                    String size = null;
                    if (!cursor.isNull(sizeIndex)) {
                        // Technically the column stores an int, but cursor.getString()
                        // will do the conversion automatically.
                        size = cursor.getString(sizeIndex);
                    } else {
                        size = "Unknown";
                    }
                    Log.i(TAG, "Size: " + size);*/

                }
            } finally {
                cursor.close();
            }
        }
        Log.d(TAG, "getFileRealPath: result= "+result);
        if (result == null) {
            result = uri.getPath();
        }
        return result;
    }

    // To pass the Uri to the camera so it can save the new photo at this directory
    public static Uri createImageUri(Context context) {
        // Create the File where the photo should go
        Uri collection = null;
        ContentResolver resolver = context.getContentResolver();

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
        values.put(MediaStore.Images.Media.TITLE, imageFileName); // Important to have a title in notifications list @api<=29
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection =  MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY); // added in @api<=29 to get the primary external storage
            //collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            //collection = Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI+ File.separator+ Environment.DIRECTORY_NOTIFICATIONS);

            // To specify a location instead of the default picture directory in external
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            //collection = MediaStore.Images.Media.getContentUriForPath(mOutputFile.getAbsolutePath());
            //values.put(MediaStore.MediaColumns.DATA, mOutputFile.getAbsolutePath()); // It crashes without the data column
        }else{
            //collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            //collection = MediaStore.Images.Media.getContentUri(String.valueOf(mActivityContext.getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS)));
            //collection = MediaStore.Images.Media.getContentUri(imageFile.getAbsolutePath());

            // To get the location of the created shared pictures storage
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File mediaFile = new File(storageDir.getAbsolutePath(), imageFileName);
            values.put(MediaStore.MediaColumns.DATA, mediaFile.getAbsolutePath()); // It crashes without the data column
        }

        if (collection == null) {
            Log.i(TAG, "writeToExternal collection is null. return");
            return null;
        }

        return resolver.insert(collection, values);

    }

    // To pass the Uri to the camera so it can save the new video at this directory
    public static Uri createVideoUri(Context context) {
        // Create the File where the photo should go
        Uri collection = null;
        ContentResolver resolver = context.getContentResolver();

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String videoFileName = "VID_" + timeStamp + ".mp4";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
        values.put(MediaStore.Video.Media.TITLE, videoFileName); // Important to have a title in notifications list @api<=29
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection =  MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY); // added in @api<=29 to get the primary external storage
            //collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            //collection = Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI+ File.separator+ Environment.DIRECTORY_NOTIFICATIONS);

            // To specify a location instead of the default picture directory in external
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);
            //collection = MediaStore.Video.Media.getContentUriForPath(mOutputFile.getAbsolutePath());
            //values.put(MediaStore.MediaColumns.DATA, mOutputFile.getAbsolutePath()); // It crashes without the data column
        }else{
            //collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            //collection = MediaStore.Video.Media.getContentUri(String.valueOf(mActivityContext.getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS)));
            //collection = MediaStore.Video.Media.getContentUri(imageFile.getAbsolutePath());

            // To get the location of the created shared pictures storage
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            File mediaFile = new File(storageDir.getAbsolutePath(), videoFileName);
            values.put(MediaStore.MediaColumns.DATA, mediaFile.getAbsolutePath()); // It crashes without the data column
        }

        if (collection == null) {
            Log.i(TAG, "collection is null. return");
            return null;
        }

        return resolver.insert(collection, values);

    }

    // To pass the Uri to the audio recorder so it can save the new alarm sound at this directory
    public static Uri addAudioToMediaStore(Context context, File outputFile) {
        // Create the File where the photo should go
        Uri collection = null;
        OutputStream outStream = null;
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, outputFile.getName());
        values.put(MediaStore.Audio.Media.TITLE, outputFile.getName()); // Important to have a title in notifications list @api<=29
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav");
        values.put(MediaStore.Audio.Media.IS_RINGTONE, false);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false); // To only appear in notifications list
        values.put(MediaStore.Audio.Media.IS_ALARM, true);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection =  MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY); // added in @api<=29 to get the primary external storage
            //collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            //collection = Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI+ File.separator+ Environment.DIRECTORY_NOTIFICATIONS);

            // To specify a location instead of the default picture directory in external
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_ALARMS);
            //collection = MediaStore.Images.Media.getContentUriForPath(mOutputFile.getAbsolutePath());
            //values.put(MediaStore.MediaColumns.DATA, mOutputFile.getAbsolutePath()); // It crashes without the data column
        }else{
            //collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            //collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            //collection = MediaStore.Images.Media.getContentUri(String.valueOf(mActivityContext.getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS)));
            //collection = MediaStore.Images.Media.getContentUri(imageFile.getAbsolutePath());

            // To get the location of the created shared pictures storage
            collection = MediaStore.Audio.Media.getContentUriForPath(outputFile.getAbsolutePath());
            values.put(MediaStore.MediaColumns.DATA, outputFile.getAbsolutePath()); // It crashes without the data column
        }

        if (collection == null) {
            Log.i(TAG, "writeToExternal collection is null. return");
            return null;
        }

        Uri uriSavedAudio = resolver.insert(collection, values);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Copy the file to a shared location because it's in the app external specific location and can't be found again by my app or others
            ParcelFileDescriptor parcelFileDescriptor;

            try {
                parcelFileDescriptor = resolver.openFileDescriptor(uriSavedAudio, "w");
                FileOutputStream outputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                FileInputStream inputStream = new FileInputStream(outputFile.getAbsoluteFile());
                FileUtils.copy(inputStream, outputStream); //Simply reads input to output stream
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                parcelFileDescriptor.close();
            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        return uriSavedAudio;
    }
}
