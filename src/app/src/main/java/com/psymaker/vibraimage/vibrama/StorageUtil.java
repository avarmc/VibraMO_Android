package com.psymaker.vibraimage.vibrama;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;



//workarounds to handle removable storage

//heavily inspired by:
//https://github.com/arpitkh96/AmazeFileManager/blob/master/src/main/java/com/amaze/filemanager/filesystem/MediaStoreHack.java
public class StorageUtil {


    public static File[] getRemovableStorageRoots(Context context) {
        File[] roots = context.getExternalFilesDirs("external");
        ArrayList<File> rootsArrayList = new ArrayList<>();

        for (int i = 0; i < roots.length; i++) {
            if (roots[i] != null) {
                String path = roots[i].getPath();
                int index = path.lastIndexOf("/Android/data/");
                if (index > 0) {
                    path = path.substring(0, index);
                    if (!path.equals(Environment.getExternalStorageDirectory().getPath())) {
                        rootsArrayList.add(new File(path));
                    }
                }
            }
        }

        roots = new File[rootsArrayList.size()];
        rootsArrayList.toArray(roots);
        return roots;
    }

    private static String getSdCardRootPath(Context context, String path) {
        File[] roots = getRemovableStorageRoots(context);
        for (int i = 0; i < roots.length; i++) {
            if (path.startsWith(roots[i].getPath())) {
                return roots[i].getPath();
            }
        }
        return null;
    }

    public static DocumentFile parseDocumentFile(Context context, Uri treeUri, File file) {
        DocumentFile treeRoot;
        try {
            treeRoot = DocumentFile.fromTreeUri(context, treeUri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }

        String path;
        try {
            path = file.getCanonicalPath();
            String sdCardPath = getSdCardRootPath(context, path);
            if (sdCardPath != null) {
                if (sdCardPath.equals(path)) {
                    return treeRoot;
                }
                path = path.substring(sdCardPath.length() + 1);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Log.d("StorageUtil", "path: " + path);

        if (treeRoot != null) {
            treeRoot = DocumentFile.fromTreeUri(context, treeUri);
            String[] pathParts = path.split("/");
            DocumentFile documentFile = treeRoot;
            for (int i = 0; i < pathParts.length; i++) {
                if (documentFile != null) {
                    documentFile = documentFile.findFile(pathParts[i]);
                } else {
                    return null;
                }
            }
            return documentFile;
        }
        return null;
    }

    public static DocumentFile createDocumentFile(Context context, Uri treeUri, String path, String mimeType) {
        int index = path.lastIndexOf("/");
        String dirPath = path.substring(0, index);
        DocumentFile file = parseDocumentFile(context, treeUri, new File(dirPath));
        if (file != null) {
            String name = path.substring(index + 1);
            file = file.createFile(mimeType, name);
        }
        return file;
    }

    public static DocumentFile createDocumentDir(Context context, Uri treeUri, String path) {
        int index = path.lastIndexOf("/");
        String dirPath = path.substring(0, index);
        DocumentFile file = parseDocumentFile(context, treeUri, new File(dirPath));
        if (file != null) {
            String name = path.substring(index + 1);
            file = file.createDirectory(name);
        }
        return file;
    }
}