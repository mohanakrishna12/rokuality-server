package com.rokuality.server.utils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.log.Log;

public class ZipUtils {

   public static boolean createZipFile(File zipFile, List<File> filesToZip) {

		Boolean success = false;
        Exception exception = null;
        
        ArrayList<File> filesToZipArrList = new ArrayList<File>(filesToZip);

        ZipFile zipMFile = null;
		try {
			zipMFile = new ZipFile(zipFile);
			ZipParameters parameters = new ZipParameters();
        	parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
            
            for (File file : filesToZipArrList) {
                if (file.isFile()) {
                    zipMFile.addFile(file, parameters);
                }

                if (file.isDirectory()) {
                    zipMFile.addFolder(file, parameters);
                }
            }
		} catch (Exception e) {
            Log.getRootLogger().warn(e);
			exception = e;
		}

		success = exception == null && zipMFile.getFile().exists();
		
        return success;
	}

	public static boolean unzipZipFile(String zipFilePath, String directoryToUnzipTo) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            zipFile.extractAll(directoryToUnzipTo);
        } catch (Exception e) {
            e.printStackTrace();
		}
		
		return true; // TODO
	}
	
	public static ZipFile zipDirectory(String directoryToZipPath) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(directoryToZipPath + ".zip");
            ZipParameters zipParams = new ZipParameters();
            zipParams.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            zipParams.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
			zipFile.addFolder(directoryToZipPath, zipParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zipFile;
    }
	

}
