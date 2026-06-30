    package com.example.myapplication.Feature.Support

    import retrofit2.http.Body
    import retrofit2.http.Headers
    import retrofit2.http.POST

    interface DialogflowApi {

        @Headers(
            "Authorization: Bearer ya29.c.c0AZ4bNpYiuvUtEuhy6Qu3S3PwuijrkErU4NEgK5TgCin3OAQGXU8_gl2T95sDEATdRbIV-Z-Ovik0FMlNFzRx54I10w7ZXUyKFnShBdr0rm7aoiOxWLoIpMMl19stvHonEmiKALyqpnd-5CAISA8_hqjqXs4W34kijNEC5YManNBEoczjriW-Jp3XCcsrVYYdO2CLMB9XNB8Yfli_2fpNlgiY2LCuHXc19Qz2_QUZL5LWqAevy9gVo_rYva7cw5XhaM07iPE8OlDZ0D7vAKqJuuMXCuETwIg1pnFpzVZZDX7nu0gDZ_M9dBetKnLhplvXNlMo2eEKFf9aeTQIP2OFr8oEj5FFgf7zXQkjv6tWDdN71CGtpAdgCo7cmVaA0oM-x1aJNwG399PeFnw8-mqIvwg4t1cfIhZlqpnrFiwXfrzs9k8Bk9fJdmbZ8aJvtdshueSyIRixkpZ-fzaziRb82u8j2cp5f9Wtxywb14xxihbaqYO1l0Jsh_Z5jVVkpB24ui_tkFd-g7twlvQS_3Z5vJ7eezJvayUfRnxWzg-0sjJJ0ak9uqut0W03BSrxubi0jh4gutl4t08leZ2BwlOZ42hRhobzUS4vp2FOhBdMJa-F4UO9vrJ0p_q3R6vk0vFQ5Jn3XyjpiqmRbojRV_B5Qq10FVF1RWVaowfv9UlaJOB7BJuMO-eMIs1qVt1crpn2S312o4kyvfgJ_dR6OWQYIOp2bVb25Bu2Zef8zn2BppXFdMMu0JungWI2Smrg2U4j_7r3vtp5mWd8Qn06sYxbVe1XpvpxRZ8IXZk805Z20Yc0Xb8lQgW62YMo9OzX-FiUO1q5kqI764ptr036Yj_VXIr5a0QoZfcUJh5mhk1R824jz26ObQZWerm3mMFxqW6s-uuoZWW_k3BVcxhtF11_po4_1W_Bi5xucwI-oI1S_R7UWde2bvdS5ysW3WOcQuIkqmX09WcF7wQMwV9VmIQMfRWSuSd_yu3nM1_cWhtufkg6_q0pVMJvWph",
            "Content-Type: application/json"
        )
        @POST("v2/projects/esp32-c9b75/agent/sessions/123456:detectIntent")
        suspend fun sendMessage(
            @Body request: DialogflowRequest
        ): DialogflowResponse
    }