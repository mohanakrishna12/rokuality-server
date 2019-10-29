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
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import java.awt.Color;
import java.awt.Image;
import java.awt.Insets;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.utils.FileToStringUtils;
import com.rokuality.server.utils.ServletJsonParser;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings({ "serial", "unchecked" })
public class manual extends HttpServlet {

	private static final int DEFAULT_TIMEOUT = 90;
	private static final String RESULTS = "results";

	private static JSONObject results = null;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
		if (response.getStatus() != HttpServletResponse.SC_OK) {
			return;
		}

		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
		case "get_start_frame":
			results = startFrame(requestObj);
			break;
		default:

			break;
		}

		if (results != null && results.containsValue(ServerConstants.SERVLET_SUCCESS)) {
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		response.setContentType("application/json");
		response.getWriter().println(results.toJSONString());
	}

	public static JSONObject startFrame(JSONObject sessionObj) {

		JSONObject results = new JSONObject();

		JFrame frame;
		
		frame = new JFrame();
		frame.setBounds(100, 100, 500, 200);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setAlwaysOnTop(true);
		frame.getContentPane().setLayout(null);

		JLabel mainLabel = new JLabel("CONNECT TO DEVICE");
		mainLabel.setBounds(20, 10, 150, 20);
		frame.getContentPane().add(mainLabel);

		JTextField serverURLField = new JTextField();
		serverURLField.setBounds(130, 48, 300, 20);
		frame.getContentPane().add(serverURLField);
		serverURLField.setColumns(10);

		JLabel serverURLLabel = new JLabel("Server URL:");
		serverURLLabel.setBounds(20, 48, 120, 20);
		frame.getContentPane().add(serverURLLabel);

		JTextField deviceIPField = new JTextField();
		deviceIPField.setBounds(130, 88, 300, 20);
		frame.getContentPane().add(deviceIPField);
		deviceIPField.setColumns(10);

		JLabel deviceIPLabel = new JLabel("Device IP:");
		deviceIPLabel.setBounds(20, 88, 120, 20);
		frame.getContentPane().add(deviceIPLabel);

		JButton btnSubmit = new JButton("Connect");
		btnSubmit.setBackground(Color.BLACK);
		btnSubmit.setForeground(Color.BLACK);
		btnSubmit.setBounds(130, 128, 100, 20);
		frame.getContentPane().add(btnSubmit);

		btnSubmit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (serverURLField.getText().isEmpty() || (deviceIPField.getText().isEmpty())) {
					// TODO - REQUIRED FIELD ERROR HANDLING
					//JOptionPane.showMessageDialog(null, "Data Missing");
				} else {
					String serverURL = serverURLField.getText();
					String deviceIP = deviceIPField.getText();
					frame.dispose();
					startSessionFrame(serverURL, deviceIP);
				}
			}
		});

		frame.setVisible(true);

		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

	public static JSONObject startSessionFrame(String serverURL, String deviceIP) {
		startSession(serverURL, deviceIP);

		JFrame frame;
		
		frame = new JFrame();
		frame.setBounds(100, 100, 1400, 1000);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setAlwaysOnTop(true);
		frame.getContentPane().setLayout(null);

		results.put("action", "get_screen_image");
		JSONObject screenImageObj = postToServer(serverURL, "screen", results);
		String imageContent = (String) screenImageObj.get("screen_image");
		String imageExt = (String) screenImageObj.get("screen_image_extension");
		File capturedImage = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator
					+ UUID.randomUUID().toString() + imageExt);
		new FileToStringUtils().convertToFile(imageContent, capturedImage);

		ImageIcon imageIcon = new ImageIcon(new ImageIcon(capturedImage.getAbsolutePath()).getImage()
				.getScaledInstance(999, 799, Image.SCALE_DEFAULT));
		JLabel imageLabel = new JLabel(imageIcon);
		imageLabel.setBounds(290, 100, 1000, 800);
		frame.add(imageLabel);

		JLabel mainLabel = new JLabel("CONTROLS");
		mainLabel.setBounds(20, 10, 150, 20);
		frame.getContentPane().add(mainLabel);

		JButton rightArrowBtn = new JButton("Right");
		rightArrowBtn.setBackground(Color.BLACK);
		rightArrowBtn.setForeground(Color.BLACK);
		rightArrowBtn.setBounds(20, 40, 50, 20);
		frame.getContentPane().add(rightArrowBtn);

		rightArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// TODO - remote action
			}
		});

		JButton leftArrowBtn = new JButton("Left");
		leftArrowBtn.setBackground(Color.BLACK);
		leftArrowBtn.setForeground(Color.BLACK);
		leftArrowBtn.setBounds(80, 40, 50, 20);
		frame.getContentPane().add(leftArrowBtn);

		leftArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// TODO - remote action
			}
		});

		JButton upArrowBtn = new JButton("Up");
		upArrowBtn.setBackground(Color.BLACK);
		upArrowBtn.setForeground(Color.BLACK);
		upArrowBtn.setBounds(140, 40, 50, 20);
		frame.getContentPane().add(upArrowBtn);

		upArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// TODO - remote action
			}
		});

		JButton downArrowBtn = new JButton("Down");
		downArrowBtn.setBackground(Color.BLACK);
		downArrowBtn.setForeground(Color.BLACK);
		downArrowBtn.setBounds(200, 40, 50, 20);
		frame.getContentPane().add(downArrowBtn);

		downArrowBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// TODO - remote action
			}
		});

		

		frame.setVisible(true);

		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

	private static boolean startSession(String serverURL, String deviceIP) {
		JSONObject sessionObj = new JSONObject();
		sessionObj.put("action", "start");
		sessionObj.put("Platform", "Roku");
		sessionObj.put("DeviceIPAddress", deviceIP);
		sessionObj.put("DeviceUsername", "rokudev"); // TODO - COME FROM FROM
		sessionObj.put("DevicePassword", "1234"); // TODO - COME FROM HOME
		sessionObj.put("AppPackage", "https://rokualitypublic.s3.amazonaws.com/RokualityDemoApp.zip"); // TODO - come from form

		results = postToServer(serverURL, "session", sessionObj);
		
		return false;
	}

	private static boolean stopSession(String serverURL) {
		results.put("action", "stop");
		results = postToServer(serverURL, "session", results);
		return false;
	}

	private static JSONObject postToServer(String serverURL, String servlet, JSONObject jsonRequest) {
        int socketTimeout = 90;
        int connectTimeout = 90;

        String responseContent = null;

        int responseCode = 500;

        HttpURLConnection con = null;
        try {
            String constructedURL = constructUrl(serverURL, servlet);
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

        if (responseCode == 401 && jsonResponseObj != null && jsonResponseObj.containsKey(RESULTS)) {
            // FAIL
        }

        if (responseCode != 200 && jsonResponseObj == null) {
            // FAIL
        }

        if (responseCode != 200 && jsonResponseObj != null && !jsonResponseObj.containsKey(RESULTS)) {
            //FAIL
        }

        return jsonResponseObj;
    }

    private static String constructUrl(String serverURL, String servletName) {
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

}