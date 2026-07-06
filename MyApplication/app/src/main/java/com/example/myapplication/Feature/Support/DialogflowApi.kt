    package com.example.myapplication.Feature.Support

    import retrofit2.http.Body
    import retrofit2.http.Headers
    import retrofit2.http.POST

    interface DialogflowApi {

        @Headers(
            "Authorization: Bearer ya29.c.c0AZ4bNpbPzm90Bz5wN3HKxYR2nvDw5xW50LXbJlS_PoLYTYWcvSDhYIDXUQH2AINM5xFK5st1L1GUDZ_hK9JJWlkA6HxNZxDp8ial8MOvarO2jmvUJ7Cni4sVO4TXRB7plzQdA7b9VPun2Yc2iBn1_0VazJZIpKIOToK_K3iBSmxSs_K3dPRiVqvwS6aTui8xigySyu3KLCU-YzDNjULQ9eYhCxHIAeZPNg14Gi_3_sLlpkf6wtEHpmfhWOnWn_8oOb8T-ong-WRpbLEhwpmIncaTgTXnD_P_JE2PQBk205pGH8TiaImnlydnsXbCa-TLnc103jdanG7Wje8hru_M7TdQyMkWcyapJ3Fg9LeWYEjqhd0lRCuQaibhKDaOGKQ0fOEtXqgN400Cfjnj9aOtVXe7iOF8rzsmUzvcr0bh6jJrX3JhbYpI2lS493s7MFdZ3lMe1InfI4x1nhwurlYxmMZn2RpIFr7FO5FI4Vl7J97lco6l-ZwXxcqcszWvfgUV9i7WfY_sk7_BShm1Q933F7ag6Rj-VJ515jRxF1MOpuj3_Zw8hpbizg0ZI9qsxxQqM_p7VlMtw_ycslZckzXed62Fys9V34g-BrvaYMzBWqfxwwekwcwSrkgScfQS6z1tMy2pzSak1XrksmMfWg7ZuOXOglynboRmsV77gMVB2w1F3a6XjsbnYRFrWafaSeuX-dQWs6tk_YzXlfJ7uqIukR-fWBwbOcd74v0vuBOd8pZij21cWF-MFFR1o0mUx0Q_QXbrpSr_J9UieshRBdl-pSU06vZjSkss0elrZc6Vf9z_BkZIra6ig1OqmyRx7rOUqa-moVejQxBUe9f-kzp4UQSIcgvu1udV3YfxqBRXslxFawX5zXlgpF6aXBkVxn-kWbFxugnsnqc0m1gSI_XqlXZl-dsi0JoZ5dz44xOWXUgwJ985xew23WObXpO4BJqgdF_MQWpmv-MBx2OrviYrUQYU18_hR6OnQYoeQRcp5fcpFcmqWaUa2_g",
            "Content-Type: application/json"
        )
        @POST("v2/projects/esp32-c9b75/agent/sessions/123456:detectIntent")
        suspend fun sendMessage(
            @Body request: DialogflowRequest
        ): DialogflowResponse
    }