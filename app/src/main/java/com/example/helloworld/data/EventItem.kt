package com.example.helloworld.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventItem(
    val id: Long,
    val slug: String,
    val title: String,
    val category: String = "General",
    @SerialName("short_description") val shortDescription: String = "",
    val description: String = "",
    val image: String = "",
    @SerialName("flyer_url") val flyerUrl: String = "",
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("all_day") val allDay: Boolean = false,
    val location: String = "",
    val address: String = "",
    @SerialName("attendance_type") val attendanceType: String = "in_person",
    @SerialName("registration_url") val registrationUrl: String = "",
    val contact: String = "",
    @SerialName("livestream_url") val livestreamUrl: String = "",
    val featured: Boolean = false,
    val status: String = "draft",
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)
