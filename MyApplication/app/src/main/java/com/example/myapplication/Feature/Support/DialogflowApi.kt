    package com.example.myapplication.Feature.Support

    import retrofit2.http.Body
    import retrofit2.http.Headers
    import retrofit2.http.POST

    interface DialogflowApi {

        @Headers(
            "Authorization: Bearer ya29.c.c0AZ4bNpa8TNSZ12aRoXiGG-dx7Z_hwYWbnC_w-9NBOT3AVPvwyCI8Kn3mXaeAsJIyYbYV9vC_5z8iITt3IjCfMrPs5-JWrFZwhstls2qHqn23iKgBksLbmkRZJjhjedryKcsFx907KHBIZnX1D8A8g2rZCuzruQydw9n0di12-Rf0lDpSoiJgR2xyNGC6lf0Xr9AlrrV1266dk6oX9uH0_OPesXM6z44LqToFsjAqfxdrf4p-4-j-fvCJZ8nnVbA6LCotdj6Ilce8Sv22Cyh6hE9xUILK-OWHz_f8ng7GBWzyzoUAf1D1LST2P92rE4aLtosuC9rabo9SLc84dcEEWX7Hy-HzRIfzy4SVKgc7-6uh0Lq8SbZQaaoE2vtFe691Z-idN397Cf0SiIByoJXX-QWsfof-f8o7Ws_F-3jfVa2nbiiOapbM--Up9mJnYu1t4V5opmFykV3_fMBbyBRz6rQuQYzoOQIJ6fIov4QIZ8jvxXeJvtwytpibyxvjuqmYRtibfFdku5Qf3X0tJgjsqZIv_FoYxb9rVj0iOu16o1QdmyhQMw-wwMapO7o2mBjwmcSOaRaIa9ktf2rFzQc_sQpx2nbUwkpsXx-b4fBB9dS5MR8j8ZrrjnZ8uqXacjS_U-dJQ4m0-5RU-cvsU7_zJBmMRBmXVRIbm2SibeSn2QsJSg5F92r8yix8iw_BqYV0F3mlF1ByaU6-p6cmZ1X7t7i62psBheXrbq-Ilsg7j6Zq3FOIo9-OJSvnhcpUYv7lR5dlQhYzXahju4r96IsSjtvu_UVeyba2tkubjBBMdFBxJoaVdnIQepduxrO9bgIszoZJ-2tWkvkypWZoccldRB4WUs50BfZnqBm0wpxq7QWdilay7bs6x_cWSavQbzoncmgZwOq-J7BQz7M6ajZS6_6Om-lOMdJjseZtjUbbp28BoW_r8ebqOp4gV_VyyQRjrZSdv0lb_YgXz7oofkzouYoJV5OafwhQIld0Ym4ijUBO60VJkVo-ueU",
            "Content-Type: application/json"
        )
        @POST("v2/projects/esp32-c9b75/agent/sessions/123456:detectIntent")
        suspend fun sendMessage(
            @Body request: DialogflowRequest
        ): DialogflowResponse
    }