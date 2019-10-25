package com.rokuality.server.core.drivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rokuality.server.constants.SessionConstants;

import org.json.simple.JSONObject;

public class ElementManager {

	private static List<Map<String, JSONObject>> elementMap = Collections.synchronizedList(new ArrayList<>());

	public static void addElement(String sessionID, JSONObject element) {
		Map<String, JSONObject> map = new HashMap<>();
		map.put(sessionID, element);
		elementMap.add(map);
	}

	public static void removeElement(String sessionID) {
		for (Map<String, JSONObject> map : elementMap) {
			if (map.containsKey(sessionID)) {
				elementMap.remove(map);
				break;
			}
		}
	}

	public static JSONObject getElement(String sessionID, String elementID) {
		for (Map<String, JSONObject> map : elementMap) {
			if (map.containsKey(sessionID) && map.get(sessionID).get(SessionConstants.ELEMENT_ID).equals(elementID)) {
				return map.get(sessionID);
			}
		}

		return null;
	}

}
