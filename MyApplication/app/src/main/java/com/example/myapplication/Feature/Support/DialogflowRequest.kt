package com.example.myapplication.Feature.Support

data class DialogflowRequest(
    val queryInput: QueryInput
)

data class QueryInput(
    val text: TextInput
)

data class TextInput(
    val text: String,
    val languageCode: String
)