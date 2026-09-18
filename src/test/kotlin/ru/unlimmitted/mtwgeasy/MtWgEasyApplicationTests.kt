package ru.unlimmitted.mtwgeasy

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class MtWgEasyApplicationTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun contextLoads() = Unit

    @Test
    fun unauthenticatedApiRequestReturnsUnauthorizedInsteadOfLoginPage() {
        mockMvc.get("/api/v1/get-wg-peers")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun loginPageRemainsPublic() {
        mockMvc.get("/login")
            .andExpect {
                status { isOk() }
            }
    }
}
