package com.rokuality.server.utils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public class ZipUtils {

   public static boolean createZipFile(File zipFile, List<File> filesToZip) {

		Boolean success = false;
		Exception exception = null;

        ZipFile zipMFile = null;
		try {
			zipMFile = new ZipFile(zipFile);
			ZipParameters parameters = new ZipParameters();
        	parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        	parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
			zipMFile.addFiles((ArrayList) filesToZip, parameters);
		} catch (Exception e) {
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
