/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import com.mopub.ormma.OrmmaView;

import android.content.Context;
import android.util.Log;

public class OrmmaAssetController extends OrmmaController {
    
    private static final String LOGTAG = "OrmmaAssetController";
    
    private static final String AD_SUBDIRECTORY_NAME = "ad";
    
    public OrmmaAssetController(OrmmaView ormmaView, Context context) {
        super(ormmaView, context);
    }
    
    /**
     * Writes the data from the specified InputStream to a file on disk. The file will be placed in 
     * either the root ad directory or a subdirectory whose name is a hash of the file contents.
     * 
     * @param is The InputStream to write to disk
     * @param filename The name of the file to write
     * @param useHashedDirectory Whether the file should be written to a hash-named subdirectory
     * @return The path to the directory where the InputStream was written
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public String writeInputStreamToDisk(InputStream is, String filename,
            boolean useHashedDirectory) throws IllegalArgumentException, IOException {
        if (is == null) throw new IllegalArgumentException("Cannot write null stream to disk.");
        if (filename == null) throw new IllegalArgumentException("Cannot write to null filename.");
        
        // Convert the input stream into a string.
        String html = new Scanner(is).useDelimiter("\\A").next();
        
        return writeHtmlToDisk(html, filename, useHashedDirectory);
    }
    
    /**
     * Writes the data from an HTML string to a file on disk. The file will be placed in 
     * either the root ad directory or a subdirectory whose name is a hash of the file contents.
     * 
     * @param html The HTML string to write to disk
     * @param filename The name of the file to write
     * @param useHashedDirectory Whether the file should be written to a hash-named subdirectory
     * @return The path to the directory where the InputStream was written
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public String writeHtmlToDisk(String html, String filename, boolean useHashedDirectory) 
            throws IllegalArgumentException, IOException {
        if (html == null) throw new IllegalArgumentException("Cannot write null string to disk.");
        if (filename == null) throw new IllegalArgumentException("Cannot write to null filename.");

        // "Fragments" are ORMMA tags that exclude <html>, <head>, and <body> tags.
        boolean isFragment = (html.indexOf("<html") == -1);
        if (isFragment) {
            html = fullHtmlFromFragment(html);
        } else {
            html = html.replace("/ormma_bridge.js", mOrmmaView.getBridgePath());
            html = html.replace("/ormma.js", mOrmmaView.getOrmmaPath());
        }
        
        String adDirPath = getAdDirPath();
        Log.d(LOGTAG, "ad dir path: " + adDirPath);
        String outFilePath = adDirPath + File.separator + filename;
        File outFile;
        
        if (!useHashedDirectory) {
            // Simply write the file to the ad directory, and return the path to the ad directory.
            outFile = writeStringToFilePath(html, outFilePath);
            return outFile.getParent();
        } else {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
                digest.update(html.getBytes());
            } catch (NoSuchAlgorithmException e) {
                // If we can't produce a hashed directory name, just write to the regular path.
                outFile = writeStringToFilePath(html, outFilePath);
                return outFile.getParent();
            }
            
            // Compute the absolute path to the new subdirectory.
            String hashedSubdirName = digestToHexString(digest);
            String newDirPath = adDirPath + File.separator + hashedSubdirName;
            
            // Write the file to the regular location, then move it to the new subdirectory.
            outFile = writeStringToFilePath(html, outFilePath);
            boolean success = moveFileToDirectory(outFile, newDirPath);
            if (success) return newDirPath;
            else throw new IOException("Could not move file to " + newDirPath);
        }
    }
    
    private String fullHtmlFromFragment(String fragment) {
        StringBuilder full = new StringBuilder("<html>");
        full.append("<head>");
        full.append("<meta name='viewport' content='user-scalable=no, initial-scale=1.0'>");
        full.append("<title>Advertisement</title>");
        full.append("<script src=\"" + mOrmmaView.getBridgePath() + "\" type=\"text/javascript\"></script>");
        full.append("<script src=\"" + mOrmmaView.getOrmmaPath() + "\" type=\"text/javascript\"></script>");
        full.append("</head>");
        full.append("<body style=\"margin:0;padding:0;overflow:hidden;background-color:transparent;\">");
        full.append("<div align=\"center\">");
        full.append(fragment);
        full.append("</div></body></html>");
        return full.toString();
    }
    
    private File writeStringToFilePath(String string, String path) throws IOException {
        File newFile = new File(path);
        if (!newFile.exists()) newFile.createNewFile();
        
        FileWriter out = new FileWriter(newFile);
        try {
            out.write(string);
            out.flush();
            out.close();
        } finally {
            if (out != null) out.close();
        }
        
        return newFile;
    }
    
    /**
     * Gets the path of the ad subdirectory where our ad assets should be kept.
     * 
     * @return The absolute path to the ad subdirectory
     */
    private String getAdDirPath() {
        String adDirPath = getFilesDir().getAbsolutePath() + File.separator + AD_SUBDIRECTORY_NAME;
        File adDir = new File(adDirPath);
        adDir.mkdir();
        return adDir.getAbsolutePath();
    }
    
    /**
     * Gets the directory where application-local files can be stored.
     * 
     * @return A File object representing the directory
     */
    private File getFilesDir() {
        return mContext.getFilesDir(); 
    }
    
    /**
     * Moves a file to the specified directory.
     * 
     * @param file The file to move
     * @param dirPath The path to the directory where the file should be moved
     * @return true on success
     */
    private boolean moveFileToDirectory(File file, String dirPath) {
        File newDir = new File(dirPath);
        newDir.mkdirs();
        boolean success = file.renameTo(new File(newDir, file.getName()));
        return success;
    }
    
    private String digestToHexString(MessageDigest digest) {
        byte messageDigest[] = digest.digest();
        
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        }
        return hexString.toString();
    }

    /* 
     * Copies a resource file from res/raw to <APP_FILE_DIRECTORY>/ad/<dstFileName>.
     */
    public String copyRawResourceToAdDirectory(int resourceId, String dstFileName) 
            throws IOException {
        InputStream is = mContext.getResources().openRawResource(resourceId);
        
        String destinationPath = getAdDirPath() + File.separator + dstFileName;
        File destinationFile = new File(destinationPath);
        FileOutputStream fos = new FileOutputStream(destinationFile);
        
        byte[] b = new byte[8192];
        try {
            for (int n; (n = is.read(b)) != -1;) {
                fos.write(b, 0, n);
            }
        } finally {
            is.close();
            fos.close();
        }
        
        return destinationPath;
    }
    
    @Override
    public void stopAllListeners() {
        // No listeners yet.
    }
    
    public void deleteCachedAds() {
        File adDir = new File(getAdDirPath());
        if (adDir.exists()) {
            try {
                delete(adDir);
            } catch (IOException e) {
                Log.e(LOGTAG, e.getMessage());
            }
        }
    }
    
    private void delete(File file) throws IOException {
        if (file.isDirectory()) {
            // We have to empty the directory before it can be deleted.
            for (File child : file.listFiles()) delete(child);
        }
        
        if (!file.delete()) {
            throw new IOException("Could not delete file: " + file.getAbsolutePath());
        }
    }
}
