package com.example.helloworld.data.offline

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "site_content_cache")
data class SiteContentEntity(
    @androidx.room.PrimaryKey val key: String,
    val value: String
)

@Entity(
    tableName = "media_items_cache",
    indices = [Index(value = ["published"]), Index(value = ["createdAt"])]
)
data class MediaItemEntity(
    @androidx.room.PrimaryKey val id: Long,
    val legacyId: Long?,
    val title: String,
    val type: String,
    val category: String,
    val description: String,
    val url: String,
    val storagePath: String?,
    val createdAt: String,
    val published: Boolean,
    val thumbnailUrl: String?,
    val featured: Boolean
)

@Entity(
    tableName = "events_cache",
    indices = [Index(value = ["status"]), Index(value = ["startAt"]), Index(value = ["displayOrder"])]
)
data class EventEntity(
    @androidx.room.PrimaryKey val id: Long,
    val slug: String,
    val title: String,
    val category: String,
    val shortDescription: String,
    val description: String,
    val image: String,
    val flyerUrl: String,
    val startAt: String,
    val endAt: String?,
    val allDay: Boolean,
    val location: String,
    val address: String,
    val attendanceType: String,
    val registrationUrl: String,
    val contact: String,
    val livestreamUrl: String,
    val featured: Boolean,
    val status: String,
    val displayOrder: Int,
    val createdAt: String,
    val updatedAt: String
)

@Entity(
    tableName = "notifications_cache",
    indices = [Index(value = ["userId"]), Index(value = ["createdAt"])]
)
data class NotificationEntity(
    @androidx.room.PrimaryKey val id: String,
    val userId: String?,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: String
)

@Entity(
    tableName = "notification_reads_cache",
    primaryKeys = ["notificationId", "userId"]
)
data class NotificationReadEntity(
    val notificationId: String,
    val userId: String,
    val readAt: String
)
