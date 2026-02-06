
package life;


import com.commontool.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Person{

public static double calculateBMI(double height, double weight){

    // 验证身高参数
    if (height <= 0.5 || height > 2.5) {
        throw new IllegalArgumentException("身高应在0.5米到2.5米之间，当前值: " + height + "米");
    }

    // 验证体重参数
    if (weight <= 20 || weight > 300) {
        throw new IllegalArgumentException("体重应在20千克到300千克之间，当前值: " + weight + "千克");
    }

    // 计算BMI：体重(kg) / 身高(m)^2
    double bmi = weight / (height * height);

    // 使用BigDecimal进行四舍五入，保留2位小数
    BigDecimal bd = new BigDecimal(bmi);
    bd = bd.setScale(2, RoundingMode.HALF_UP);

    return bd.doubleValue();
}

    /**
     * 获取BMI分类
     * @param bmi BMI指数
     * @return BMI分类描述
     */
    public static  String getBMICategory(double bmi) {
        if (bmi < 18.5) {
            return "偏瘦";
        } else if (bmi < 24) {
            return "正常";
        } else if (bmi < 28) {
            return "超重";
        } else {
            return "肥胖";
        }
    }

    // 测试用例
    public static void main(String[] args) {
        try {
            // 正常情况测试
            System.out.println("测试1（正常）:");
            double bmi1 = calculateBMI(1.75, 70);
            System.out.printf("身高1.75米，体重70千克 -> BMI: %.2f (%s)%n",
                    bmi1, getBMICategory(bmi1));

            System.out.println("\n测试2（边界值）:");
            double bmi2 = calculateBMI(1.6, 45);
            System.out.printf("身高1.6米，体重45千克 -> BMI: %.2f (%s)%n",
                    bmi2, getBMICategory(bmi2));

            // 异常情况测试
            System.out.println("\n测试3（异常身高）:");
            calculateBMI(0.3, 70); // 这会抛出异常

        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }

        try {
            System.out.println("\n测试4（异常体重）:");
            calculateBMI(1.75, 5); // 这会抛出异常
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
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
