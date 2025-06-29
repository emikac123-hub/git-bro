package com.erik.git_bro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiModelController.class)
public class AiModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void test() {
        // TODO: Add tests
    }
}
