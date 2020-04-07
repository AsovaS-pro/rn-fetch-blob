package com.RNFetchBlob.Utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.os.Environment;
import android.content.ContentResolver;
import com.RNFetchBlob.RNFetchBlobUtils;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import android.util.Log;

public class PathResolver {
    @TargetApi(19)
    public static String getRealPathFromURI(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            Log.w("TAG OLOLOL", "111111");
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                Log.w("TAG OLOLOL", "1111112222222");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    Log.w("TAG OLOLOL", "11111133333333");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                Log.w("TAG OLOLOL", "2222222");
                try {
                    final String id = DocumentsContract.getDocumentId(uri);

                    if (id != null && id.startsWith("raw:/")) {
                        Uri rawuri = Uri.parse(id);
                        String path = rawuri.getPath();
                        return path;
                    }

                    // final Uri contentUri = ContentUris.withAppendedId(
                    //         Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

                    // String[] contentUriPrefixesToTry = new String[]{
                    //     "content://downloads/public_downloads",
                    //     "content://downloads/my_downloads",
                    //     "content://downloads/all_downloads"
                    // };

                    // for (String contentUriPrefix : contentUriPrefixesToTry) {
                    //     Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                    //     try {
                    //         String path = getDataColumn(context, contentUri, null, null);
                    //         if (path != null) {
                    //             Log.w("TAG OLOLOL", path);
                    //             return path;
                    //         }
                    //     } catch (Exception e) {
                    //         Log.w("TAG OLOLOL111111111111", e.getLocalizedMessage());
                    //     }
                    // }
                        // return getDataColumn(context, contentUri, null, null);
                    }
                catch (Exception ex) {
                    Log.w("TAG OLOLOL", ex.getLocalizedMessage());
                    Log.w("TAG OLOLOL", "NULL 11111");
                    //something went wrong, but android should still be able to handle the original uri by returning null here (see readFile(...))
                    return null;
                }
                
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                Log.w("TAG OLOLOL", "isMediaDocument(uri)");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
Log.w("TAG OLOLOL", "equalsIgnoreCase(uri.getScheme())");
                // Return the remote address
                if (isGooglePhotosUri(uri))
                    return uri.getLastPathSegment();

                return getDataColumn(context, uri, null, null);
            }
            // Other Providers
            else{
                Log.w("TAG OLOLOL", "Other Providers");
                try {
                    InputStream attachment = context.getContentResolver().openInputStream(uri);
                    if (attachment != null) {
                        String filename = getContentName(context.getContentResolver(), uri);
                        if (filename != null) {
                            File file = new File(context.getCacheDir(), filename);
                            FileOutputStream tmp = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            while (attachment.read(buffer) > 0) {
                                tmp.write(buffer);
                            }
                            tmp.close();
                            attachment.close();
                            return file.getAbsolutePath();
                        }
                    }
                } catch (Exception e) {
                    RNFetchBlobUtils.emitWarningEvent(e.toString());
                    Log.w("TAG OLOLOL", "NULL 2222222");
                    return null;
                }
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
Log.w("TAG OLOLOL", "MediaStore (and general)");
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Log.w("TAG OLOLOL", "// File");
            return uri.getPath();
        }
Log.w("TAG OLOLOL", "NULL 333333");
        return null;
    }

    private static String getContentName(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            String name = cursor.getString(nameIndex);
            cursor.close();
            return name;
        }
        Log.w("TAG OLOLOL", "NULL 444444");
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        String result = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
Log.w("GET_DATA_COLUMN uri = ", uri.toString());
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                result = cursor.getString(index);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Log.w("TAG OLOLOL", "NULL 55555");
            Log.e("MYAPP OLOLOL", "exception", ex);
            return null;
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
        return result;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}
