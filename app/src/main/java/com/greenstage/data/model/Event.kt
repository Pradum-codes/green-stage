package com.greenstage.data.model

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val venue: String,
    val startAt: Long,
    val endAt: Long,
    val imageUrl: String?,
    val communityId: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPublished: Boolean
)
