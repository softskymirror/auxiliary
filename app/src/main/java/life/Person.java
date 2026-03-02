
package life;


import com.commontool.JSONUtils;
import com.sqltool.FieldInfo;
import com.sqltool.MySQLUtils;
import com.sqltool.TableInfo;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author softskymirror
 */
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



public void getPersonInfos(){

}

public void initializeData(){
    // 示例1: 创建数据库和表
    System.out.println("=== 示例1: 创建数据库和表 ===");

    // 连接到MySQL服务器（不指定数据库）
    MySQLUtils dbUtil = new MySQLUtils("localhost", 3306, "mysql", "root", "684428");

    // 测试连接
    if (!dbUtil.testConnection()) {
        System.err.println("数据库连接失败，请检查连接信息");
        return;
    }

    // 创建测试数据库
    dbUtil.createDatabase("test_db");

    // 切换到新数据库
    dbUtil.useDatabase("test_db");

    // 创建表结构定义
    FieldInfo idField = new FieldInfo();
    idField.setColumnName("id");
    idField.setDataType(FieldInfo.TYPE_INT);
    idField.setLength(11);
    idField.setUnsigned(true);
    idField.setNotNull(true);
    idField.setAutoIncrement(true);

    FieldInfo nameField = new FieldInfo();
    nameField.setColumnName("name");
    nameField.setDataType(FieldInfo.TYPE_VARCHAR);
    nameField.setLength(50);
    nameField.setNotNull(true);

    FieldInfo priceField = new FieldInfo();
    priceField.setColumnName("telephone");
    priceField.setDataType(FieldInfo.TYPE_DECIMAL);
    priceField.setPrecision(10);
    priceField.setScale(2);
    priceField.setDefaultValue("0.00");

// 2. 构建表信息
    TableInfo relationshipTable = new TableInfo();
    relationshipTable.setTableName("products");
    relationshipTable.setEngine("InnoDB");
    relationshipTable.setCharacterSet("utf8mb4");
    relationshipTable.setCollate("utf8mb4_general_ci");
    relationshipTable.setPrimaryKeys(Arrays.asList("id")); // 单列主键
    relationshipTable.getFields().add(idField);
    relationshipTable.getFields().add(nameField);
    relationshipTable.getFields().add(priceField);


// 3. 创建表

    dbUtil.createTable(relationshipTable);

    // 创建用户表
//            dbUtil.createTable("users", columns, "id", "InnoDB");

    // 示例2: 插入数据
    System.out.println("\n=== 示例2: 插入数据 ===");

    // 插入单条数据
//            Map<String, Object> user1 = new HashMap<>();
//            user1.put("name", "张三");
//            user1.put("age", 25);
//            user1.put("email", "zhangsan@example.com");
//            dbUtil.insert("users", user1);
    Map<String, Object> data = new HashMap<>();
    data.put("name", "张三");
    data.put("age", 25);
    int rows = insert(userTable, data); // 自动使用表名，并可验证字段
    // 插入多条数据
    Map<String, Object> user2 = new HashMap<>();
    user2.put("name", "李四");
    user2.put("age", 30);
    user2.put("email", "lisi@example.com");
    dbUtil.insert("users", user2);

    // 批量插入数据
    String[] userColumns = {"name", "age", "email"};
    List<Object[]> userData = new ArrayList<>();
    userData.add(new Object[]{"王五", 28, "wangwu@example.com"});
    userData.add(new Object[]{"赵六", 35, "zhaoliu@example.com"});
    dbUtil.batchInsert("users", userColumns, userData);

    // 示例3: 查询数据
    System.out.println("\n=== 示例3: 查询数据 ===");

    // 查询所有数据
    List<Map<String, Object>> allUsers = dbUtil.select("users", null, null);
    System.out.println("所有用户:");
    for (Map<String, Object> user : allUsers) {
        System.out.println(user);
    }

    // 条件查询
    List<Map<String, Object>> youngUsers = dbUtil.select(
            "users",
            new String[]{"id", "name", "age"},
            "age < ?",
            30
    );
    System.out.println("\n年龄小于30的用户:");
    for (Map<String, Object> user : youngUsers) {
        System.out.println(user);
    }

    // 示例4: 更新数据
    System.out.println("\n=== 示例4: 更新数据 ===");

    Map<String, Object> updateData = new HashMap<>();
    updateData.put("age", 31);
    updateData.put("email", "lisi_updated@example.com");

    int updatedRows = dbUtil.update(
            "users",
            updateData,
            "name = ?",
            "李四"
    );
    System.out.println("更新了 " + updatedRows + " 行数据");

    // 示例5: 删除数据
    System.out.println("\n=== 示例5: 删除数据 ===");

    int deletedRows = dbUtil.delete(
            "users",
            "name = ?",
            "赵六"
    );
    System.out.println("删除了 " + deletedRows + " 行数据");

    // 示例6: 表操作
    System.out.println("\n=== 示例6: 表操作 ===");

}

public void getJsonType(){
    ArrayList<HashMap<Integer, HashMap<String,Object>>> maps=new ArrayList<>();
    HashMap<String,Object> per_properties=new HashMap<>();
    per_properties.put("name","黄锐楠");
    per_properties.put("age",25);
    per_properties.put("height",168.9);
    per_properties.put("we",168.9);
    HashMap<Integer,HashMap<String,Object>> map=new HashMap<>();
    map.put(JSONUtils.OBJECT_DATA,per_properties);
    maps.add(map);
    System.out.println(JSONUtils.generateJson(maps));

}

    public static void testDemo(String[] args) {
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
}
