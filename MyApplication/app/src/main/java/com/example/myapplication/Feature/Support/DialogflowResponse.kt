package com.example.myapplication.Feature.Support

data class DialogflowResponse(
    val queryResult: QueryResult
)

data class QueryResult(
    val queryText: String = "",
    val fulfillmentText: String,
    val intent: IntentInfo?,
    val parameters: Map<String, Any>? = null
)

data class IntentInfo(
    val displayName: String
)