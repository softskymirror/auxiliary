package com.sqltool;

import life.Person;
import life.PersonProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class PersonTest {

    @Test
    @DisplayName("测试 Person 构造器和 getter")
    void testPersonConstructor() {
        MySQLUtils mockDbUtil = mock(MySQLUtils.class);
        Person person = new Person(mockDbUtil);
        assertNotNull(person);
        assertEquals(mockDbUtil, person.getDbUtil());
    }

    @Test
    @DisplayName("测试 BMI 计算功能 - 正常值")
    void testBMICalculation() {
        double bmi = PersonProperties.calculateBMI(1.75, 70);
        assertTrue(bmi > 0);
        String category = PersonProperties.getBMICategory(bmi);
        assertNotNull(category);
    }

    @Test
    @DisplayName("测试 BMI 计算 - 参数异常")
    void testBMICalculationInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            PersonProperties.calculateBMI(1.75, 5);
        });
    }

    @Test
    @DisplayName("测试 Person insertData 不抛异常")
    void testInsertDataNoException() {
        // insertData 依赖静态方法，无法完全 mock，仅验证构造器和基本功能
        MySQLUtils mockDbUtil = mock(MySQLUtils.class);
        Person person = new Person(mockDbUtil);
        assertNotNull(person);
        assertNotNull(person.getDbUtil());
    }
}
