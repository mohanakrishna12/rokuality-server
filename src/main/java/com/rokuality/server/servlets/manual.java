package com.rokuality.server.servlets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import java.awt.*;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.enums.PlatformType;
import com.rokuality.server.enums.RokuButton;
import com.rokuality.server.enums.XBoxButton;
import com.rokuality.server.utils.FileToStringUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.SleepUtils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings({ "serial", "unchecked" })
public class manual extends HttpServlet {

	private static JSONObject sessionObj = null;
	private static String platform = "";
	private static String serverURL = "";
	private static String deviceIP = "";
	private static String appPackage = "";
	private static String app = "";
	private static String username = "";
	private static String password = "";
	private static String harmonyIP = "";
	private static String deviceName = "";
	private static String errorTxt = "";
	private static boolean sessionStarted = false;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		initManualSession();
		response.setStatus(HttpServletResponse.SC_OK);
	}

	public static void initManualSession() {
		sessionStarted = false;
		sessionObj = new JSONObject();

		JFrame frame = new JFrame();
		frame.setBounds(100, 100, 500, 400);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setAlwaysOnTop(true);
		frame.getContentPane().setLayout(null);

		JLabel mainLabel = new JLabel("CONNECT TO DEVICE");
		mainLabel.setBounds(20, 10, 150, 20);
		mainLabel.setFont(new Font("Helvetica", Font.BOLD, 12));
		frame.getContentPane().add(mainLabel);

		JLabel platformLabel = new JLabel("Platform:");
		platformLabel.setBounds(20, 48, 120, 20);
		frame.getContentPane().add(platformLabel);

		JLabel usernameLabel = new JLabel("Username:");
		usernameLabel.setBounds(20, 208, 120, 20);
		frame.getContentPane().add(usernameLabel);
		usernameLabel.setVisible(false);

		JTextField usernameField = new JTextField();
		usernameField.setBounds(130, 208, 300, 20);
		frame.getContentPane().add(usernameField);
		usernameField.setColumns(10);
		usernameField.setText(username);
		usernameField.setVisible(false);

		JLabel passwordLabel = new JLabel("Password:");
		passwordLabel.setBounds(20, 248, 120, 20);
		frame.getContentPane().add(passwordLabel);
		passwordLabel.setVisible(false);

		JTextField passwordField = new JTextField();
		passwordField.setBounds(130, 248, 300, 20);
		frame.getContentPane().add(passwordField);
		passwordField.setColumns(10);
		passwordField.setText(password);
		passwordField.setVisible(false);

		JLabel appLabel = new JLabel("App:");
		appLabel.setBounds(20, 208, 120, 20);
		frame.getContentPane().add(appLabel);
		appLabel.setVisible(false);

		JTextField appField = new JTextField();
		appField.setBounds(130, 208, 300, 20);
		frame.getContentPane().add(appField);
		appField.setColumns(10);
		appField.setText(app);
		appField.setVisible(false);

		JLabel harmonyLabel = new JLabel("Harmony IP:");
		harmonyLabel.setBounds(20, 248, 120, 20);
		frame.getContentPane().add(harmonyLabel);
		harmonyLabel.setVisible(false);

		JTextField harmonyField = new JTextField();
		harmonyField.setBounds(130, 248, 300, 20);
		frame.getContentPane().add(harmonyField);
		harmonyField.setColumns(10);
		harmonyField.setText(harmonyIP);
		harmonyField.setVisible(false);

		JLabel deviceLabel = new JLabel("Device Name:");
		deviceLabel.setBounds(20, 288, 120, 20);
		frame.getContentPane().add(deviceLabel);
		deviceLabel.setVisible(false);

		JTextField deviceField = new JTextField();
		deviceField.setBounds(130, 288, 300, 20);
		frame.getContentPane().add(deviceField);
		deviceField.setColumns(10);
		deviceField.setText(deviceName);
		deviceField.setVisible(false);

		JButton btnSubmit = new JButton("Connect");
		btnSubmit.setForeground(Color.BLACK);
		btnSubmit.setBounds(20, 328, 100, 20);
		frame.getContentPane().add(btnSubmit);
		btnSubmit.setVisible(false);

		JComboBox<String> comboBox = new JComboBox<String>();
		comboBox.addItem("--Select--");
		comboBox.addItem("Roku");
		comboBox.addItem("XBox");
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnSubmit.setVisible(true);

				usernameLabel.setVisible(false);
				usernameField.setVisible(false);
				passwordLabel.setVisible(false);
				passwordField.setVisible(false);
				harmonyLabel.setVisible(false);
				harmonyField.setVisible(false);
				deviceLabel.setVisible(false);
				deviceField.setVisible(false);
				appLabel.setVisible(false);
				appField.setVisible(false);

				if (comboBox.getSelectedItem().toString().equals("Roku")) {
					usernameLabel.setVisible(true);
					usernameField.setVisible(true);
					passwordLabel.setVisible(true);
					passwordField.setVisible(true);
				}

				if (comboBox.getSelectedItem().toString().equals("XBox")) {
					appLabel.setVisible(true);
					appField.setVisible(true);
					harmonyLabel.setVisible(true);
					harmonyField.setVisible(true);
					deviceLabel.setVisible(true);
					deviceField.setVisible(true);
				}
			}
		});
		comboBox.setBounds(130, 48, 150, 20);
		frame.getContentPane().add(comboBox);

		JLabel serverURLLabel = new JLabel("Server URL:");
		serverURLLabel.setBounds(20, 88, 120, 20);
		frame.getContentPane().add(serverURLLabel);

		JTextField serverURLField = new JTextField();
		serverURLField.setBounds(130, 88, 300, 20);
		frame.getContentPane().add(serverURLField);
		serverURLField.setColumns(10);
		serverURLField.setText(serverURL);

		JLabel deviceIPLabel = new JLabel("Device IP:");
		deviceIPLabel.setBounds(20, 128, 120, 20);
		frame.getContentPane().add(deviceIPLabel);

		JTextField deviceIPField = new JTextField();
		deviceIPField.setBounds(130, 128, 300, 20);
		frame.getContentPane().add(deviceIPField);
		deviceIPField.setColumns(10);
		deviceIPField.setText(deviceIP);

		JLabel appPackageLabel = new JLabel("App Package:");
		appPackageLabel.setBounds(20, 168, 120, 20);
		frame.getContentPane().add(appPackageLabel);

		JButton btnAppPackage = new JButton("Choose");
		btnAppPackage.setForeground(Color.BLACK);
		btnAppPackage.setBounds(130, 168, 100, 20);
		frame.getContentPane().add(btnAppPackage);

		JTextField appPackageField = new JTextField();
		appPackageField.setBounds(235, 168, 195, 20);
		frame.getContentPane().add(appPackageField);
		appPackageField.setColumns(10);

		btnAppPackage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JFileChooser fileChooser = new JFileChooser();
				int returnValue = fileChooser.showOpenDialog(null);
				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					appPackage = selectedFile.getAbsolutePath();
					appPackageField.setText(appPackage);
				}
			}
		});

		JLabel preparingDeviceLabel = new JLabel("Preparing device connection...");
		preparingDeviceLabel.setBounds(130, 328, 200, 20);
		frame.getContentPane().add(preparingDeviceLabel);
		preparingDeviceLabel.setVisible(false);

		JLabel errorLabel = new JLabel(errorTxt);
		errorLabel.setBounds(20, 348, 499, 20);
		errorLabel.setBackground(Color.RED);
		errorLabel.setForeground(Color.RED);
		frame.getContentPane().add(errorLabel);
		errorLabel.setVisible(false);

		btnSubmit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				errorLabel.setVisible(false);
				if (serverURLField.getText().isEmpty() || appPackageField.getText().isEmpty()) {
					errorTxt = "The Server URL and App Package fields are required!";
					errorLabel.setText(errorTxt);
					errorLabel.setVisible(true);
				} else {
					serverURL = serverURLField.getText();
					deviceIP = deviceIPField.getText();
					platform = comboBox.getSelectedItem().toString();
					username = usernameField.getText();
					password = passwordField.getText();
					harmonyIP = harmonyField.getText();
					deviceName = deviceField.getText();
					app = appField.getText();

					File appPackageFile = new File(appPackage);
					if (appPackageFile.exists() && appPackageFile.isFile()) {
						appPackage = new FileToStringUtils().convertToString(appPackageFile);
					}

					preparingDeviceLabel.setVisible(true);

					new Thread() {
						@Override
						public void run() {
							sessionStarted = startSession();
							if (!sessionStarted) {
								errorTxt = sessionObj == null ? "An error occurred! Verify your server url location"
										: (String) sessionObj.get(ServerConstants.SERVLET_RESULTS);
								errorLabel.setText(errorTxt);
								errorLabel.setVisible(true);
								preparingDeviceLabel.setVisible(false);
							}

							if (sessionStarted) {
								frame.dispose();
								startSessionFrame();
							}
						};
					}.start();
				}
			}
		});

		frame.setVisible(true);
	}

	public static void startSessionFrame() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenWidth = (int) screenSize.getWidth();
		int screenHeight = (int) screenSize.getHeight();

		JFrame frame = new JFrame();
		frame.setBounds(1, 1, screenWidth, screenHeight);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setAlwaysOnTop(true);
		frame.getContentPane().setLayout(null);

		JLabel imageLabel = new JLabel("Preparing screen image...");
		imageLabel.setBounds(290, 10, screenWidth - 300, screenHeight - 20);
		frame.add(imageLabel);

		new Thread() {
			@Override
			public void run() {
				while (sessionStarted) {
					File capturedImage = getImage();
					if (capturedImage != null && capturedImage.exists()) {
						ImageIcon icn = new ImageIcon(new ImageIcon(capturedImage.getAbsolutePath()).getImage()
								.getScaledInstance(screenWidth - 300, screenHeight - 20, Image.SCALE_DEFAULT));
						imageLabel.setIcon(icn);
						FileUtils.deleteFile(capturedImage);
					}
				}
			};
		}.start();

		JLabel mainLabel = new JLabel("CONTROLS");
		mainLabel.setBounds(20, 10, 150, 20);
		frame.getContentPane().add(mainLabel);

		if (PlatformType.ROKU.value().equalsIgnoreCase(platform)) {
			addRokuControlPanel(frame);
		}

		if (PlatformType.XBOX.value().equalsIgnoreCase(platform)) {
			addXBoxControlPanel(frame);
		}
		
		JButton newSessionBtn = new JButton("New Session");
		newSessionBtn.setForeground(Color.BLACK);
		newSessionBtn.setBounds(20, 400, 150, 20);
		frame.getContentPane().add(newSessionBtn);

		newSessionBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				sessionStarted = false;
				stopSession();
				frame.dispose();
				initManualSession();
			}
		});

		frame.setVisible(true);

		new Thread() {
			@Override
			public void run() {
				while (frame.isDisplayable()) {
					SleepUtils.sleep(1000);
				}

				sessionStarted = false;
				stopSession();
				frame.dispose();
			};
		}.start();
	}

	private static boolean startSession() {
		sessionObj = new JSONObject();
		sessionObj.put("action", "start");
		sessionObj.put("Platform", platform);
		sessionObj.put("DeviceIPAddress", deviceIP);
		sessionObj.put("AppPackage", appPackage);

		if (PlatformType.ROKU.value().equalsIgnoreCase(platform)) {
			sessionObj.put("DeviceUsername", username);
			sessionObj.put("DevicePassword", password);
		}

		if (PlatformType.XBOX.value().equalsIgnoreCase(platform)) {
			sessionObj.put("HomeHubIPAddress", harmonyIP);
			sessionObj.put("DeviceName", deviceName);
			sessionObj.put("App", app);
		}
		
		sessionObj = postToServer("session", sessionObj);

		return sessionObj != null && sessionObj.containsValue(ServerConstants.SERVLET_SUCCESS);
	}

	private static void stopSession() {
		sessionObj.put("action", "stop");
		postToServer("session", sessionObj);
	}

	private static JSONObject postToServer(String servlet, JSONObject jsonRequest) {
		int socketTimeout = 90;
		int connectTimeout = 90;

		String responseContent = null;

		int responseCode = 500;

		HttpURLConnection con = null;
		try {
			String constructedURL = constructUrl(servlet);
			con = (HttpURLConnection) new URL(constructedURL).openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setConnectTimeout((connectTimeout * 1000));
			con.setReadTimeout((socketTimeout * 1000));

			try (OutputStream outputStream = con.getOutputStream()) {
				byte[] input = jsonRequest.toJSONString().getBytes("utf-8");
				outputStream.write(input, 0, input.length);
			} catch (Exception e) {
				e.printStackTrace();
			}

			responseCode = con.getResponseCode();
			
			InputStreamReader inputStreamReader = null;
			if (responseCode >= 200 && responseCode < 400) {
				inputStreamReader = new InputStreamReader(con.getInputStream(), "utf-8");
			} else {
				inputStreamReader = new InputStreamReader(con.getErrorStream(), "utf-8");
			}

			try (BufferedReader br = new BufferedReader(inputStreamReader)) {
				StringBuilder response = new StringBuilder();
				String responseLine = null;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
				responseContent = response.toString();
				br.close();
			}

			if (inputStreamReader != null) {
				inputStreamReader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (con != null) {
				con.disconnect();
			}
		}

		JSONParser jsonParser = new JSONParser();
		JSONObject jsonResponseObj = null;

		if (responseContent != null) {
			try {
				jsonResponseObj = (JSONObject) jsonParser.parse(responseContent);
			} catch (ParseException e) {
				System.out.println(String.format("Response content %s is not valid json!", responseContent));
			}
		}

		return jsonResponseObj;
	}

	private static String constructUrl(String servletName) {
		String finalizedURL = null;

		String baseURL = serverURL;
		String queryString = "";
		if (serverURL.contains("?")) {
			String[] urlComps = serverURL.split("\\?");
			baseURL = urlComps[0];
			if (urlComps.length >= 2) {
				queryString = "?" + urlComps[1];
			}
		}

		finalizedURL = baseURL + "/" + servletName + queryString;
		return finalizedURL;
	}

	private static File getImage() {
		sessionObj.put("action", "get_screen_image");
		JSONObject screenImageObj = postToServer("screen", sessionObj);
		String imageContent = (String) screenImageObj.get("screen_image");
		String imageExt = (String) screenImageObj.get("screen_image_extension");
		File capturedImage = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator
				+ UUID.randomUUID().toString() + imageExt);
		if (imageContent == null) {
			return null;
		}

		File imageFile = new FileToStringUtils().convertToFile(imageContent, capturedImage);
		if (!imageFile.exists()) {
			return null;
		}
		return imageFile;
	}

	private static void pressButton(String button) {
		sessionObj.put("action", "press_button");
		sessionObj.put("remote_button", button);
		postToServer("remote", sessionObj);
	}

	private static void sendKeys(String keys) {
		sessionObj.put("action", "send_keys");
		sessionObj.put("text", keys);
		postToServer("remote", sessionObj);
	}

	private static void addRokuControlPanel(JFrame frame) {
		JButton backBtn = new JButton("←");
		backBtn.setForeground(Color.BLACK);
		backBtn.setBounds(20, 40, 50, 20);
		frame.getContentPane().add(backBtn);

		backBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.BACK.value());
			}
		});

		JButton upArrowBtn = new JButton("^");
		upArrowBtn.setForeground(Color.BLACK);
		upArrowBtn.setBounds(80, 80, 50, 20);
		frame.getContentPane().add(upArrowBtn);

		upArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.UP_ARROW.value());
			}
		});

		JButton leftArrowBtn = new JButton("<");
		leftArrowBtn.setForeground(Color.BLACK);
		leftArrowBtn.setBounds(20, 120, 50, 20);
		frame.getContentPane().add(leftArrowBtn);

		leftArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.LEFT_ARROW.value());
			}
		});

		JButton selectBtn = new JButton("OK");
		selectBtn.setForeground(Color.BLACK);
		selectBtn.setBounds(75, 120, 60, 20);
		frame.getContentPane().add(selectBtn);

		selectBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.SELECT.value());
			}
		});

		JButton rightArrowBtn = new JButton(">");
		rightArrowBtn.setForeground(Color.BLACK);
		rightArrowBtn.setBounds(140, 120, 50, 20);
		frame.getContentPane().add(rightArrowBtn);

		rightArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.RIGHT_ARROW.value());
			}
		});

		JButton downArrowBtn = new JButton("∨");
		downArrowBtn.setForeground(Color.BLACK);
		downArrowBtn.setBounds(80, 160, 50, 20);
		frame.getContentPane().add(downArrowBtn);

		downArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.DOWN_ARROW.value());
			}
		});

		JButton optionsBtn = new JButton("*");
		optionsBtn.setForeground(Color.BLACK);
		optionsBtn.setBounds(120, 200, 50, 20);
		frame.getContentPane().add(optionsBtn);

		optionsBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.OPTION.value());
			}
		});

		JButton rewindBtn = new JButton("<<");
		rewindBtn.setForeground(Color.BLACK);
		rewindBtn.setBounds(20, 240, 50, 20);
		frame.getContentPane().add(rewindBtn);

		rewindBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.REWIND.value());
			}
		});

		JButton playPauseBtn = new JButton("▷||");
		playPauseBtn.setForeground(Color.BLACK);
		playPauseBtn.setBounds(75, 240, 60, 20);
		frame.getContentPane().add(playPauseBtn);

		playPauseBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.PLAY.value());
			}
		});

		JButton fastForwardBtn = new JButton(">>");
		fastForwardBtn.setForeground(Color.BLACK);
		fastForwardBtn.setBounds(140, 240, 50, 20);
		frame.getContentPane().add(fastForwardBtn);

		fastForwardBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(RokuButton.FAST_FORWARD.value());
			}
		});

		JTextField sendKeysField = new JTextField();
		sendKeysField.setBounds(20, 280, 125, 20);
		frame.getContentPane().add(sendKeysField);
		sendKeysField.setColumns(10);

		JButton sendKeysBtn = new JButton("Type");
		sendKeysBtn.setForeground(Color.BLACK);
		sendKeysBtn.setBounds(150, 280, 75, 20);
		frame.getContentPane().add(sendKeysBtn);

		sendKeysBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				sendKeys(sendKeysField.getText());
				sendKeysField.setText("");
			}
		});
	}

	private static void addXBoxControlPanel(JFrame frame) {
		JButton upArrowBtn = new JButton("^");
		upArrowBtn.setForeground(Color.BLACK);
		upArrowBtn.setBounds(70, 40, 50, 20);
		frame.getContentPane().add(upArrowBtn);

		upArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.UP_ARROW.value());
			}
		});

		JButton leftArrowBtn = new JButton("<");
		leftArrowBtn.setForeground(Color.BLACK);
		leftArrowBtn.setBounds(30, 80, 50, 20);
		frame.getContentPane().add(leftArrowBtn);

		leftArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.LEFT_ARROW.value());
			}
		});

		JButton rightArrowBtn = new JButton(">");
		rightArrowBtn.setForeground(Color.BLACK);
		rightArrowBtn.setBounds(110, 80, 50, 20);
		frame.getContentPane().add(rightArrowBtn);

		rightArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.RIGHT_ARROW.value());
			}
		});

		JButton downArrowBtn = new JButton("v");
		downArrowBtn.setForeground(Color.BLACK);
		downArrowBtn.setBounds(70, 120, 50, 20);
		frame.getContentPane().add(downArrowBtn);

		downArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.DOWN_ARROW.value());
			}
		});

		JButton yBtn = new JButton("Y");
		yBtn.setForeground(Color.BLACK);
		yBtn.setBounds(70, 200, 50, 20);
		frame.getContentPane().add(yBtn);

		yBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.Y.value());
			}
		});

		JButton xBtn = new JButton("X");
		xBtn.setForeground(Color.BLACK);
		xBtn.setBounds(30, 240, 50, 20);
		frame.getContentPane().add(xBtn);

		rightArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.X.value());
			}
		});

		JButton bBtn = new JButton("B");
		bBtn.setForeground(Color.BLACK);
		bBtn.setBounds(110, 240, 50, 20);
		frame.getContentPane().add(bBtn);

		bBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.B.value());
			}
		});

		JButton aBtn = new JButton("A");
		aBtn.setForeground(Color.BLACK);
		aBtn.setBounds(70, 280, 50, 20);
		frame.getContentPane().add(aBtn);

		aBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pressButton(XBoxButton.A.value());
			}
		});
	}

}