package com.smallnine.apiserver;

import com.smallnine.apiserver.dao.UserDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class MyBatisConfigTest {

    @Autowired(required = false)
    private UserDao userDao;

    @Test
    public void contextLoads() {
        // 檢查Spring上下文是否成功加載
    }

    @Test
    public void userDaoShouldBeAvailable() {
        // 檢查UserDao是否正確注入
        assertThat(userDao).isNotNull();
    }
}