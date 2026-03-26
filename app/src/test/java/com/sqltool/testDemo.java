package com.sqltool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // 启用 Mockito 注解
public class PersonTest {

    @Mock
    private MySQLUtils mockDbUtil;   // 模拟数据库工具类

    @InjectMocks
    private Person person;            // 自动将 mockDbUtil 注入到 Person 构造器

    @Test
    void testSavePerson() {
        // 当调用 insert 方法时，返回 1（模拟影响行数）
        when(mockDbUtil.insert(anyString(), any())).thenReturn(1);

        // 执行业务方法
        person.savePerson("李四", 30);

        // 验证 insert 方法被调用了一次，且参数包含 "李四" 和 30
        verify(mockDbUtil, times(1)).insert(anyString(), any());
    }
}