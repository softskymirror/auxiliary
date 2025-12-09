package com.adbtool.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class JsonUtil {
    public static JSONObject generateJson(String key, ArrayList<HashMap<String, Object>> lists) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonstring = new JSONObject();
        for (int l = 0; l < lists.size(); l++) {
            HashMap<String, Object> infos = lists.get(l);
            Set<String> infos_str = infos.keySet();
            for (Iterator iterator = infos_str.iterator(); iterator.hasNext(); ) {
                Object str = iterator.next();
                jsonObject.put(str, infos.get(str));
                //给json数据加上key
            }
            jsonstring.put(key, jsonObject);
        }
        return jsonstring;
    }
}
