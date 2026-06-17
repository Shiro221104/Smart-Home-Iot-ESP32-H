    package com.example.myapplication.Feature.Support

    import retrofit2.http.Body
    import retrofit2.http.Headers
    import retrofit2.http.POST

    interface DialogflowApi {

        @Headers(
            "Authorization: Bearer ya29.c.c0AZ4bNpbt7shSWVCyoiPIfwLiP9DQipFIYad4cYFdweBFTgBkq9zTr4I6o0U5eV_n0BrDkShcm9x-iKdd3FIi0JMSdfDLDpICiTuimkYNtqgbRTbMEpiPI18wq7t6c93XOk298w4uuAoKfWcthABzMvcGWUBlTldtqzWcfxrQcyRgzlqgEF6z_w-BvsuZRvBZeD7MyqiaPHsIxYJ2stLzDTr_3Fbo6kxlgLFPPU_1hRheu8uCu7UfS8uEmGExQ-9ulaCbHxsA0GUUPXSL-WiQcNRIdCuRGbefkDWmmQUR_T6bx9LI5-Ekruzmb_Pb-qXqWlTQTv0CEDVD8l96Vs-pxaJzqkc1F0Z0lcMhm_3qTAgJhFvAxf-Eh_hcIaeUgTVkSZzuMy8G400KeWJ1X9npv0F6rqWatVfQIgzxXgU64qSBSB0ZSWemJbVZYUogmQnlIhsq_14lxrOb5qwu9jSO7vyWd17WqQf4bIj3-XjQmMr9SgW6qRdldhFR6nyuycqIuZnpSeQMc3FinYU_UoOwVek9ieo4-Oyj1IUlUt-WX-WBa7_BX69m-Wd_SsJdaamtys1zgbg2fsoywqhIBldrhn5W6_kwscM9oa-g0-8t5QOrZX3jnJgSlFIWUcnXiuZcJWzueonu6d9O1Y-vmtgXMennj99FqFoWR7grhy1UdUwkFkn8F10sa9MMreM4Uuf7aUVOho29Qf69uJM4xg8fQ82QSBdz1I0pvi0yFzU9XJmpFbn6dQ-V1-yydxrbFqYJlmbd4ohQO-h-ReF6mx2yUlnO--eb0i8FXMW27JIUkyjUqb_Oa4U5evu8ipl4cg2ZamymjigMfswxdt85v1s041aMU70XRrznewsXtsR0w-Btfm2Ir6Io5IRRB70zVZ6lcyz6_84acVQu9nw8wtZzugIggUfcob4a6Fa_inbpSIlXyFge9-j3vcfzh27Z3eudMoek83ZiRQiUlp1m8rd1sUc__37q1J02dMapSri9XdI51dQv0VOcfIF",
            "Content-Type: application/json"
        )
        @POST("v2/projects/esp32-c9b75/agent/sessions/123456:detectIntent")
        suspend fun sendMessage(
            @Body request: DialogflowRequest
        ): DialogflowResponse
    }