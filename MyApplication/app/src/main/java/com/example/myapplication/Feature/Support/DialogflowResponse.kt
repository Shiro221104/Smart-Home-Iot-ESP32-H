package com.example.myapplication.Feature.Support

data class DialogflowResponse(
    val queryResult: QueryResult
)

data class QueryResult(
    val fulfillmentText: String,
    val intent: IntentInfo?
)

data class IntentInfo(
    val displayName: String
)