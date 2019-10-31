package com.rokuality.server.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.main.ServerMain;

@SuppressWarnings({ "serial" })
public class tools extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringBuilder html = new StringBuilder();
		html.append("<html>");
		html.append("<h3>Tools</h3>");
		html.append("<a href='http://localhost:" + ServerMain.getServerPort()
				+ "/manual' target='_blank'>Start manual session</a>");
		html.append("</html>");

		response.setContentType("text/html");
		response.getWriter().println(html.toString());
		response.setStatus(HttpServletResponse.SC_OK);
	}

}