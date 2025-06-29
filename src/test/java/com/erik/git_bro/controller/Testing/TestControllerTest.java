package com.erik.git_bro.controller.Testing;

import com.erik.git_bro.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TestController.class)
public class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewRepository reviewRepository;

    @Test
    @WithMockUser
    public void testPostReviewData() throws Exception {
        mockMvc.perform(post("/test/review-data").with(csrf())).andExpect(status().isOk());
    }
}
