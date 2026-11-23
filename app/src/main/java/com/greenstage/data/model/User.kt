package com.greenstage.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val accountType: AccountType,
    val interests: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)
