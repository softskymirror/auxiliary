package com.commontool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class JsonUtils {
    public final static int OBJECT_DATA=1;
    public final static int CHA_SET_DATA=2;
    public final static int ARR_DATA=3;
    public  static JSONObject generateJson(ArrayList<HashMap<Integer,HashMap<String,Object>>> maps) {
//      JSONArray jsonArray = new JSONArray();
        JSONObject jsonString = new JSONObject();
        for (int i = 0; i < maps.size(); i++){
            HashMap<Integer, HashMap<String, Object>> map_infos = maps.get(i);
                Set<Integer> infos = map_infos.keySet();
                for (Iterator<Integer> iterator = infos.iterator(); iterator.hasNext(); ) {
                    int key = iterator.next();
                    HashMap<String, Object> map = map_infos.get(key);
                    Set<String> info = map.keySet();
                    switch (key) {
                        case OBJECT_DATA:
                            for (Iterator<String> _info = info.iterator(); iterator.hasNext(); ) {
                                Object inf = info.iterator();
                                jsonString.put(inf, map.get(inf));
                            }
                            break;
                        case CHA_SET_DATA:
                            JSONObject jsonObject = new JSONObject();
                            Object cha_key = new Object();
                            for (Iterator<String> _info = info.iterator(); iterator.hasNext(); ) {
                                String set_cha = _info.next();
                                if (set_cha.equals("key")) cha_key = map.get(set_cha);
                                else jsonObject.put(set_cha, map.get(set_cha));
                            }
                            jsonString.put(key, jsonObject.toString());
                            break;
                        case ARR_DATA:

                            break;

                    }

            }

        }
        return jsonString;
    }

        /**
     * ����JSONObject����
     * @param bytes
     * @return
     */
    public static HashMap<String,Object> parseJSONOject(byte[] bytes){
        String json = new String(bytes);
        HashMap<String, Object> list = new HashMap<>();
        JSONObject respJson = JSONObject.fromObject(json);
        Set<String> strs = respJson.keySet();
        for(Iterator iterator = strs.iterator(); iterator.hasNext();) {
            String str = (String) iterator.next();
            list.put(str, respJson.getString(str));
        }
        return list;
    }
}
