package com.commontool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class JsonUtils {
    public static JSONObject generateJson(HashMap<String,Object> maps,String list_key, ArrayList<HashMap<String, Object>> lists) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonString = new JSONObject();
        for(int i=0;i<maps.size();i++){
            Set<String> map_infos = maps.keySet();
            for (Iterator iterator =  map_infos.iterator(); iterator.hasNext(); ) {
                Object str = iterator.next();
                jsonString.put(str, maps.get(str));
                //给json数据加上key
        }
    }
        for (int l = 0; l < lists.size(); l++) {
            HashMap<String, Object> infos = lists.get(l);
            Set<String> infos_str = infos.keySet();
            JSONObject jsonObject=new JSONObject();
            for (Iterator iterator =  infos_str.iterator(); iterator.hasNext(); ) {
                Object str = iterator.next();
                jsonObject.put(str, infos.get(str));
                //给json数据加上key
            }
            jsonArray.add(jsonObject.toString());
        }
        jsonString.put(list_key, jsonArray.toString());
        return  jsonString;
    }

        /**
     * 解析JSONObject对象
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
