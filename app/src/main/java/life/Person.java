
package life;


import com.commontool.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class Person{

public void calmBMI(){


}

public void getPersonInfos(){

}

public void getPersonJson(){
    ArrayList<HashMap<Integer, HashMap<String,Object>>> maps=new ArrayList<>();
    HashMap<String,Object> per_properties=new HashMap<>();
    per_properties.put("name","黄锐楠");
    per_properties.put("age",25);
    per_properties.put("height",168.9);
    per_properties.put("we",168.9);
    HashMap<Integer,HashMap<String,Object>> map=new HashMap<>();
    map.put(JsonUtils.OBJECT_DATA,per_properties);
    maps.add(map);
    System.out.println(JsonUtils.generateJson(maps));

}
}