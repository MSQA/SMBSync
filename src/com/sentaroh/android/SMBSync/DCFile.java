package com.sentaroh.android.SMBSync;

import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("NewApi")
class DCFile {
    private Context mContext;
    private Uri mUri;

    DCFile(DCFile parent, Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    public static DCFile fromTreeUri(Context context, Uri treeUri) {
    	return new DCFile(null, context, prepareTreeUri(treeUri));
    }
    
    public static Uri prepareTreeUri(Uri treeUri) {
        return DocumentsContract.buildDocumentUriUsingTree(treeUri,
                DocumentsContract.getTreeDocumentId(treeUri));
    }

    
    public DCFile createFile(String mimeType, String displayName) {
        final Uri result = DocumentsContract.createDocument(mContext.getContentResolver(), mUri, mimeType,
                displayName);
        return (result != null) ? new DCFile(this, mContext, result) : null;
    }

    public DCFile createDirectory(String displayName) {
        final Uri result = DocumentsContract.createDocument(mContext.getContentResolver(), mUri, 
        		DocumentsContract.Document.MIME_TYPE_DIR, displayName);
        return (result != null) ? new DCFile(this, mContext, result) : null;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getName() {
        return queryForString(mContext, mUri, DocumentsContract.Document.COLUMN_DISPLAY_NAME, null);
    }

    public String getType() {
        final String rawType = getRawType(mContext, mUri);
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(rawType)) {
            return null;
        } else {
            return rawType;
        }
    }
    
    private static String getRawType(Context context, Uri mUri) {
        return queryForString(context, mUri, DocumentsContract.Document.COLUMN_MIME_TYPE, null);
    }


    public boolean isDirectory() {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(getRawType(mContext, mUri));
    }

    public boolean isFile() {
        final String type = getRawType(mContext, mUri);
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(type) || TextUtils.isEmpty(type)) {
            return false;
        } else {
            return true;
        }
    }

    public long lastModified() {
        return queryForLong(mContext, mUri, DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0);
    }

    public long length() {
        return queryForLong(mContext, mUri, DocumentsContract.Document.COLUMN_SIZE, 0);
    }

    public boolean canRead() {
        // Ignore if grant doesn't allow read
        if (mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Ignore documents without MIME
        if (TextUtils.isEmpty(getRawType(mContext, mUri))) {
            return false;
        }

        return true;

    }

    public boolean canWrite() {
        // Ignore if grant doesn't allow write
        if (mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        final String type = getRawType(mContext, mUri);
        final int flags = queryForInt(mContext, mUri, DocumentsContract.Document.COLUMN_FLAGS, 0);

        // Ignore documents without MIME
        if (TextUtils.isEmpty(type)) {
            return false;
        }

        // Deletable documents considered writable
        if ((flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0) {
            return true;
        }

        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(type)
                && (flags & DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0) {
            // Directories that allow create considered writable
            return true;
        } else if (!TextUtils.isEmpty(type)
                && (flags & DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0) {
            // Writable normal files considered writable
            return true;
        }

        return false;
    }

    public boolean delete() {
        return DocumentsContract.deleteDocument(mContext.getContentResolver(), mUri);
    }

    public boolean exists() {
        final ContentResolver resolver = mContext.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(mUri, new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID }, null, null, null);
            return c.getCount() > 0;
        } catch (Exception e) {
            Log.w("DCFile", "Failed query: " + e);
            return false;
        } finally {
            closeQuietly(c);
        }
    }

    public DCFile[] listFiles() {
        final Uri[] result = listDocUris();
        
        final DCFile[] resultFiles = new DCFile[result.length];
        for (int i = 0; i < result.length; i++) {
            resultFiles[i] = new DCFile(this, mContext, result[i]);
        }
        return resultFiles;
    }
    
    private Uri[] listDocUris() {
        final ContentResolver resolver = mContext.getContentResolver();
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mUri,
                DocumentsContract.getDocumentId(mUri));
        final ArrayList<Uri> results = new ArrayList<Uri>();

        Cursor c = null;
        try {
            c = resolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID }, null, null, null);
            while (c.moveToNext()) {
                final String documentId = c.getString(0);
                final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(mUri,
                        documentId);
                results.add(documentUri);
            }
        } catch (Exception e) {
            Log.w("DCFile", "Failed query: " + e);
        } finally {
            closeQuietly(c);
        }

        return results.toArray(new Uri[results.size()]);
    }

    public boolean renameTo(String displayName) {
        final Uri result = DocumentsContract.renameDocument(mContext.getContentResolver(), mUri, displayName);
        if (result != null) {
            mUri = result;
            return true;
        } else {
            return false;
        }
    }
    
    private static String queryForString(Context context, Uri self, String column,
            String defaultValue) {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[] { column }, null, null, null);
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            Log.w("DCFile", "Failed query: " + e);
            return defaultValue;
        } finally {
            closeQuietly(c);
        }
    }

    private static int queryForInt(Context context, Uri self, String column,
            int defaultValue) {
        return (int) queryForLong(context, self, column, defaultValue);
    }

    private static long queryForLong(Context context, Uri self, String column,
            long defaultValue) {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[] { column }, null, null, null);
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getLong(0);
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            Log.w("DCFile", "Failed query: " + e);
            return defaultValue;
        } finally {
            closeQuietly(c);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

}