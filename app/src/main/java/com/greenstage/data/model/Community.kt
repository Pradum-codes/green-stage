package com.greenstage.data.model

data class Community(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)
