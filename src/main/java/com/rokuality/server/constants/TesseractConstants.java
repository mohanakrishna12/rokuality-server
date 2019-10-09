package com.rokuality.server.constants;

import java.io.File;

public class TesseractConstants {

	public static final String TESS_DATA_NAME = "tessdata_v4.0";
	public static final String TESS_DATA_ZIP_NAME = TESS_DATA_NAME + ".zip";
	public static final File TESSERACT_TESS_DATA_DIR = new File(DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() 
			+ File.separator + TESS_DATA_NAME);

}
