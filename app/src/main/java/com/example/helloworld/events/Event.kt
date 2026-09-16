package com.example.helloworld.events

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: Long = 0,
    val slug: String = "",
    val title: String = "",
    val category: String = "General",
    val short_description: String = "",
    val description: String = "",
    val image: String = "",
    val flyer_url: String = "",
    val start_at: String = "",
    val end_at: String? = null,
    val all_day: Boolean = false,
    val location: String = "",
    val address: String = "",
    val attendance_type: String = "in_person",
    val registration_url: String = "",
    val contact: String = "",
    val livestream_url: String = "",
    val featured: Boolean = false,
    val status: String = "published",
    val display_order: Int = 0
)

@Serializable
data class EventInput(
    val title: String,
    val category: String = "General",
    val short_description: String = "",
    val description: String = "",
    val image: String = "",
    val flyer_url: String = "",
    val start_at: String,
    val end_at: String? = null,
    val all_day: Boolean = false,
    val location: String = "",
    val address: String = "",
    val attendance_type: String = "in_person",
    val registration_url: String = "",
    val contact: String = "",
    val livestream_url: String = "",
    val featured: Boolean = false,
    val status: String = "draft",
    val display_order: Int = 0
)
