

package com.commontool;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * General JSON Data Encapsulation Class
 */
public class JsonUtils {
   //Object常量判断数据
    public final static int OBJECT_DATA=1;
    //Object常量判断数据
    public final static int CHA_SET_DATA=2;
    //
    public final static int ARR_DATA=3;

    /**
     *The basic logic of this function is to process all the encapsulated data in the HashMap according to the data structure type.
     *The corresponding structure of the data is as follows：
     * OBJECT_DATA {<str_name>:(Object)<string|integer|boolean>}
     * CHA_SET_DATA  <str_root>:{<str_name>:(Object)<string|integer|boolean>,<str_name>:(Object)<string|integer|boolean>,......}
     * ARR_DATA <str_root>:[(Object), (Object), (Object)]
     * @param maps
     * @return
     */
    public  static JSONObject generateJson(ArrayList<HashMap<Integer,HashMap<String,Object>>> maps) {
//      JSONArray jsonArray = new JSONArray();
        JSONObject jsonString = new JSONObject();
        for (int i = 0; i < maps.size(); i++){
            HashMap<Integer, HashMap<String, Object>> map_infos = maps.get(i);
                Set<Integer> infos = map_infos.keySet();
                //Iterate over all the HashMap key parameters to determine the parameter types
                for (Iterator<Integer> iterator = infos.iterator(); iterator.hasNext(); ) {
                    int key = iterator.next();
                    HashMap<String, Object> map = map_infos.get(key);
                    Set<String> info = map.keySet();
                    switch (key) {
                        case OBJECT_DATA:
                            for (Iterator<String> _info = info.iterator(); _info.hasNext(); ) {
                                Object inf = _info.next();
                                jsonString.put(inf, map.get(inf));
                            }
                            break;
                        case CHA_SET_DATA:
                            JSONObject jsonObject = new JSONObject();
                            Object cha_key = new Object();
                            for (Iterator<String> _info = info.iterator(); _info.hasNext(); ) {
                                String set_cha = _info.next();
                                if (set_cha.equals("key")) cha_key = map.get(set_cha);
                                else jsonObject.put(set_cha, map.get(set_cha));
                            }
                            jsonString.put(cha_key, jsonObject.toString());
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
