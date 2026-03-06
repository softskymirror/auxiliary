package life;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PersonProperties {
    private float height;
    private float weight;
    private int age;
    private float BMI;
    private String hometown;
    private String career;
    private String body_health;
    private String name;
    private String online_name;
    private String sexual_orientation;

    private final static double weight_limit = 99;
    private final static double weight_loss = 47;
    private final static double height_limit = 0.5;
    private final static double height_loss = 2.5;

    public void setHeight(float height) {
        this.height = height;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setBMI(float BMI) {
        this.BMI = BMI;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public void setCareer(String career) {
        this.career = career;
    }

    public void setBody_health(String body_health) {
        this.body_health = body_health;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOnline_name(String online_name) {
        this.online_name = online_name;
    }

    public void setSexual_orientation(String sexual_orientation) {
        this.sexual_orientation = sexual_orientation;
    }

    /**
     * 计算BMI指数
     *
     * @param height 身高（单位：米）
     * @param weight 体重（单位：千克）
     * @return BMI指数（四舍五入保留2位小数）
     * @throws IllegalArgumentException 当参数不在合理范围内时抛出异常
     */
    public static double calculateBMI(double height, double weight) {
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
     *
     * @param bmi BMI指数
     * @return BMI分类描述
     */
    public static String getBMICategory(double bmi) {
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
}
