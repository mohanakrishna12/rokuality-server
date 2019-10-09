package com.rokuality.server.core.drivers;

import org.json.simple.JSONObject;

import java.util.*;

import com.rokuality.server.constants.SessionConstants;

public class ElementManager {

	private static List<Map<String, JSONObject>> elementMap = Collections.synchronizedList(new ArrayList<>());

	public static void addElement(String sessionID, JSONObject element) {
		Map<String, JSONObject> map = new HashMap<>();
		map.put(sessionID, element);
		elementMap.add(map);
	}

	public static void removeElement(String sessionID) {
		Map<String, JSONObject> map;
		for (int i = 0; i < elementMap.size(); i++) {
			map = elementMap.get(i);
			if (map.containsKey(sessionID)) {
				elementMap.remove(i);
				break;
			}
		}
	}

	public static JSONObject getElement(String sessionID, String elementID) {
		Map<String, JSONObject> map;
		for (int i = 0; i < elementMap.size(); i++) {
			map = elementMap.get(i);

			if (map.containsKey(sessionID) && map.get(sessionID).get(SessionConstants.ELEMENT_ID).equals(elementID)) {
				return map.get(sessionID);
			}
		}

		return null;
	}

}
