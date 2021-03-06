package com.rokuality.server.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.rokuality.server.enums.OSType;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.utils.CacheUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.OSUtils;

import org.eclipse.jetty.util.log.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class OSUtils {

	private static OSType osType = null;
	public static String userPathComplete = null;
	public static String javaHome = null;

	public static Boolean isMac() {
		return getOS().contains("mac");
	}

	public static Boolean isWindows() {
		return getOS().contains("win");
	}

	public static Boolean isLinux() {
		String os = getOS();
		return os.contains("nix") || os.contains("nux") || os.contains("aix");
	}

	public static OSType getOSType() {
		if (osType != null) {
			return osType;
		}

		if (isMac()) {
			osType = OSType.MAC;
		}

		if (isWindows()) {
			osType = OSType.WINDOWS;
		}

		if (isLinux()) {
			osType = OSType.LINUX;
		}

		return osType;
	}

	private static String getOS() {
		return System.getProperty("os.name").toLowerCase();
	}

	public static File getUserBaseDir() {
		return new File(System.getProperty("user.home") + File.separator + DependencyConstants.ROKUALITY_NAME);
	}

	public static String getBinaryPath(String binaryName) {
		String binPath = "";

		String cacheProp = binaryName + "-path-location-prop";
		String cacheValue = CacheUtils.get(cacheProp);
		if (cacheValue != null && !cacheValue.equals("")) {
			return cacheValue;
		}

		CommandExecutor commandExecutor = new CommandExecutor();

		File binaryScriptPath = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + binaryName
				+ "path_" + System.currentTimeMillis() + ".sh");
		String[] command = null;
		String commandInput = null;

		if (!OSUtils.isWindows()) {
			String userPath = getPathVar();
			commandInput = "# !/bin/bash" + System.lineSeparator() + "export PATH=" + userPath + System.lineSeparator()
					+ "RESULTS=$(which " + binaryName + ")" + System.lineSeparator() + "echo \"${RESULTS}\"";
			command = new String[] { DependencyConstants.SHELL_EXECUTOR, binaryScriptPath.getAbsolutePath() };
		}

		if (OSUtils.isWindows()) {
			binaryScriptPath = new File(binaryScriptPath.getAbsolutePath().replace(".sh", ".bat"));
			commandInput = "@echo off" + System.lineSeparator() + "set arg1=%1" + System.lineSeparator()
					+ "where.exe %arg1%";
			command = new String[] { DependencyConstants.SHELL_EXECUTOR, binaryScriptPath.getAbsolutePath(),
					binaryName };
		}

		boolean fileCreated = FileUtils.writeStringToFile(binaryScriptPath, commandInput, true);
		if (fileCreated) {
			binPath = commandExecutor.execCommand(String.join(" ", command), null);
		}

		if (binPath == null) {
			Log.getRootLogger().warn("Failed to query for binary '" + binaryName + "' in user's path.");
			return "";
		}
		binPath = binPath.trim();

		if (binPath.contains(System.lineSeparator())) {
			Log.getRootLogger()
					.info(String.format(
							"Multiple binary %s matches found in path with results '%s'. The first match will be used!",
							binaryName, binPath));
			String[] binPathComps = binPath.split(System.lineSeparator());
			binPath = binPathComps[0];
		}

		File resultFile = new File(binPath);
		if (!resultFile.exists() && !resultFile.getName().contains(binaryName)) {
			Log.getRootLogger().warn("Did not find binary '" + binaryName + "' in user's path.", new Object[] {});
			binPath = "";
		} else {
			Log.getRootLogger().info("Binary path '" + binaryName + "' found at: " + binPath, new Object[] {});
			CacheUtils.add(cacheProp, binPath);
		}
		FileUtils.deleteFile(binaryScriptPath);

		return binPath;
	}

	public static String getPathVar() {
		if (userPathComplete == null) {
			String path = System.getenv("PATH");
			javaHome = System.getenv("JAVA_HOME");

			if (OSUtils.isLinux()) {
				path = path + ":" + File.separator + "sbin";
			}

			userPathComplete = "\"" + path + "\"";
			Log.getRootLogger().info("User path: " + userPathComplete);
		}

		return userPathComplete;
	}

	public static void displaySystemMessage(String message) {
		if (isLinux()) {
			// linux messaging not yet supported
			return;
		}

		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setShape(new RoundRectangle2D.Double(1, 1, 345, 95, 50, 50));
		frame.setSize(345, 95);
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setBackground(Color.WHITE);
		frame.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0f;
		constraints.weighty = 1.0f;
		constraints.insets = new Insets(15, 15, 5, 5);
		constraints.fill = GridBagConstraints.BOTH;

		JLabel headingLabel = new JLabel(" " + "Rokuality Server");
		headingLabel.setFont(new Font("Helvetica", Font.BOLD, 17));

		BufferedImage image = null;
		try (InputStream inputStream = OSUtils.class.getResourceAsStream("/serverimg.jpg")) {
			image = ImageIO.read(inputStream);
		} catch (IOException e) {
			Log.getRootLogger().warn(e);
		}
		ImageIcon imageIcon = new ImageIcon(image.getScaledInstance(30, 30, Image.SCALE_DEFAULT));

		headingLabel.setIcon(imageIcon);
		headingLabel.setOpaque(false);
		headingLabel.setForeground(Color.BLACK);
		frame.add(headingLabel, constraints);
		constraints.gridx++;
		constraints.weightx = 0f;
		constraints.weighty = 0f;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.gridx = 0;
		constraints.gridy++;
		constraints.weightx = 1.0f;
		constraints.weighty = 1.0f;
		constraints.insets = new Insets(5, 15, 15, 15);
		constraints.fill = GridBagConstraints.BOTH;
		JLabel messageLabel = new JLabel("<HtMl>" + "  " + message);
		messageLabel.setFont(new Font("Helvetica", Font.PLAIN, 14));
		messageLabel.setForeground(Color.BLACK);
		frame.add(messageLabel, constraints);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets toolHeight = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
		int frameX = ((scrSize.width - frame.getWidth()) - 10);
		int frameY = (scrSize.height - toolHeight.bottom - frame.getHeight()) - 10;
		frame.setLocation(frameX, frameY);
		frame.setAlwaysOnTop(true);
		new Thread() {
			@Override
			public void run() {
				SleepUtils.sleep(5000);
				frame.dispose();
			};
		}.start();
	}

}
