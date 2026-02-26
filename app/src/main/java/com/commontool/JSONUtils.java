package com.commontool;


import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**

 *      *The basic logic of this function is to process all the encapsulated data in the HashMap according to the data structure type.
 *      *The corresponding structure of the data is as follows：
 *      * OBJECT_DATA {<str_name>:(Object)<string|integer|boolean>}
 *      * CHA_SET_DATA  <str_root>:{<str_name>:(Object)<string|integer|boolean>,<str_name>:(Object)<string|integer|boolean>,......}
 *      * ARR_DATA <str_root>:[(Object), (Object), (Object)]
 *      * @param maps
 *      * @return
 *      */
public class JSONUtils {
   //Object常量判断数据
    public final static int OBJECT_DATA=1;
    //Object常量判断数据
    public final static int CHA_SET_DATA=2;
    //
    public final static int ARR_DATA=3;

    /**
     * 将复杂数据结构转换为 JSONObject
     * @param maps 数据列表，每个元素为一个 HashMap，其键为数据类型，值为该类型的数据内容
     * @return 生成的 JSONObject
     */
    public static JSONObject generateJson(ArrayList<HashMap<Integer, HashMap<String, Object>>> maps) {
        JSONObject result = new JSONObject();
        if (maps == null || maps.isEmpty()) {
            return result;
        }

        for (HashMap<Integer, HashMap<String, Object>> mapInfo : maps) {
            if (mapInfo == null) continue;

            // 遍历每个类型条目（通常只有一个）
            for (Map.Entry<Integer, HashMap<String, Object>> entry : mapInfo.entrySet()) {
                int dataType = entry.getKey();
                HashMap<String, Object> dataMap = entry.getValue();
                if (dataMap == null) continue;

                switch (dataType) {
                    case OBJECT_DATA:
                        handleObjectData(result, dataMap);
                        break;
                    case CHA_SET_DATA:
                        handleChaSetData(result, dataMap);
                        break;
                    case ARR_DATA:
                        handleArrData(result, dataMap);
                        break;
                    default:
                        // 忽略未知类型，可打印日志
                        break;
                }
            }
        }
        return result;
    }

    /** 处理普通对象数据：直接添加所有字段到结果 */
    private static void handleObjectData(JSONObject result, HashMap<String, Object> dataMap) {
        for (Map.Entry<String, Object> field : dataMap.entrySet()) {
            String fieldName = field.getKey();
            Object fieldValue = convertToJsonCompatible(field.getValue());
            result.put(fieldName, fieldValue);
        }
    }

    /** 处理特性集数据：提取 key 作为字段名，其余字段组成子对象 */
    private static void handleChaSetData(JSONObject result, HashMap<String, Object> dataMap) {
        Object keyObj = dataMap.get("key");
        if (keyObj == null) return; // 缺少 key，跳过

        String key = keyObj.toString(); // 确保键为字符串
        JSONObject subObject = new JSONObject();

        for (Map.Entry<String, Object> field : dataMap.entrySet()) {
            String fieldName = field.getKey();
            if ("key".equals(fieldName)) continue; // 跳过 key 本身

            Object fieldValue = convertToJsonCompatible(field.getValue());
            subObject.put(fieldName, fieldValue);
        }

        result.put(key, subObject);
    }

    /** 处理数组数据：提取 key 和 items，items 应为 List 或数组，转换为 JSONArray */
    private static void handleArrData(JSONObject result, HashMap<String, Object> dataMap) {
        Object keyObj = dataMap.get("key");
        if (keyObj == null) return;

        String key = keyObj.toString();
        Object items = dataMap.get("items");

        JSONArray jsonArray = convertToJsonArray(items);
        result.put(key, jsonArray);
    }

    /** 递归将任意对象转换为 JSON 兼容类型（Map -> JSONObject，Collection/数组 -> JSONArray） */
    private static Object convertToJsonCompatible(Object obj) {
        if (obj == null) {
            return JSONObject.NULL; // org.json 中表示 null 的方式
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "null" : entry.getKey().toString();
                Object value = convertToJsonCompatible(entry.getValue());
                jsonObject.put(key, value);
            }
            return jsonObject;
        } else if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            JSONArray jsonArray = new JSONArray();
            for (Object item : collection) {
                jsonArray.put(convertToJsonCompatible(item));
            }
            return jsonArray;
        } else if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            JSONArray jsonArray = new JSONArray();
            for (Object item : array) {
                jsonArray.put(convertToJsonCompatible(item));
            }
            return jsonArray;
        } else {
            // 基本类型或已兼容类型（String, Number, Boolean）直接返回
            return obj;
        }
    }

    /** 将对象转换为 JSONArray，支持 Collection、数组或单个对象 */
    private static JSONArray convertToJsonArray(Object obj) {
        if (obj == null) {
            return new JSONArray();
        }
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            JSONArray jsonArray = new JSONArray();
            for (Object item : collection) {
                jsonArray.put(convertToJsonCompatible(item));
            }
            return jsonArray;
        } else if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            JSONArray jsonArray = new JSONArray();
            for (Object item : array) {
                jsonArray.put(convertToJsonCompatible(item));
            }
            return jsonArray;
        } else {
            // 单个对象作为数组元素
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(convertToJsonCompatible(obj));
            return jsonArray;
        }
    }


//    public  static JSONObject generateJson(ArrayList<HashMap<Integer,HashMap<String,Object>>> maps) {
////      JSONArray jsonArray = new JSONArray();
//        JSONObject jsonString = new JSONObject();
//        for (int i = 0; i < maps.size(); i++){
//            HashMap<Integer, HashMap<String, Object>> map_infos = maps.get(i);
//                Set<Integer> infos = map_infos.keySet();
//                //Iterate over all the HashMap key parameters to determine the parameter types
//                for (Iterator<Integer> iterator = infos.iterator(); iterator.hasNext(); ) {
//                    int key = iterator.next();
//                    HashMap<String, Object> map = map_infos.get(key);
//                    Set<String> info = map.keySet();
//                    switch (key) {
//                        case OBJECT_DATA:
//                            for (Iterator<String> _info = info.iterator(); _info.hasNext(); ) {
//                                Object inf = _info.next();
//                                jsonString.put(inf, map.get(inf));
//                            }
//                            break;
//                        case CHA_SET_DATA:
//                            JSONObject jsonObject = new JSONObject();
//                            Object cha_key = new Object();
//                            for (Iterator<String> _info = info.iterator(); _info.hasNext(); ) {
//                                String set_cha = _info.next();
//                                if (set_cha.equals("key")) cha_key = map.get(set_cha);
//                                else jsonObject.put(set_cha, map.get(set_cha));
//                            }
//                            jsonString.put(cha_key, jsonObject.toString());
//                            break;
//                        case ARR_DATA:
//
//                            break;
//
//                    }
//
//            }
//
//        }
//        return jsonString;
//    }



//    public static HashMap<String,Object> parseJSONOject(byte[] bytes){
//        String json = new String(bytes);
//        HashMap<String, Object> list = new HashMap<>();
//        JSONObject respJson = JSONObject.fromJson(json, JSONObject.class);
//        Set<String> strs = respJson.keySet();
//        for(Iterator iterator = strs.iterator(); iterator.hasNext();) {
//            String str = (String) iterator.next();
//            list.put(str, respJson.getString(str));
//        }
//        return list;
//    }

    public static void testDemo(){
        ArrayList<HashMap<Integer, HashMap<String, Object>>> input = new ArrayList<>();

// 添加一个普通对象
        HashMap<Integer, HashMap<String, Object>> objBlock = new HashMap<>();
        HashMap<String, Object> objData = new HashMap<>();
        objData.put("name", "Alice");
        objData.put("age", 30);
        objBlock.put(OBJECT_DATA, objData);
        input.add(objBlock);

// 添加一个特性集
        HashMap<Integer, HashMap<String, Object>> chaBlock = new HashMap<>();
        HashMap<String, Object> chaData = new HashMap<>();
        chaData.put("key", "address");
        chaData.put("city", "New York");
        chaData.put("zip", 10001);
        chaBlock.put(CHA_SET_DATA, chaData);
        input.add(chaBlock);

// 添加一个数组
        HashMap<Integer, HashMap<String, Object>> arrBlock = new HashMap<>();
        HashMap<String, Object> arrData = new HashMap<>();
        arrData.put("key", "scores");
        arrData.put("items", Arrays.asList(95, 87, 92));
        arrBlock.put(ARR_DATA, arrData);
        input.add(arrBlock);
        JSONObject json = JSONUtils.generateJson(input);
        System.out.println(json.toString(2));
    }
 }

