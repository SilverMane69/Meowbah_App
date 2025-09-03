package com.kawaii.meowbah.data

import android.graphics.Bitmap // ADDED import

// A simplified data class for widget video items
data class WidgetCompatibleVideo(
    val id: String, // Video ID
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    @Transient var thumbnailBitmap: Bitmap? = null, // To hold the downloaded bitmap
    val publishedAt: String? = null // ADDED: To hold the published date
)

object SampleVideos {
    // This is a MOCK list. In a real scenario, you'd fetch this from a repository or database.
    val MOCK_VIDEO_LIST: List<WidgetCompatibleVideo> = listOf(
        WidgetCompatibleVideo(
            id = "dQw4w9WgXcQ", 
            title = "Meowbah's First Vlog!",
            description = "Welcome to my channel! Join me on my first adventure as a VTuber.",
            thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            publishedAt = "2023-01-01T12:00:00Z" // Example date
        ),
        WidgetCompatibleVideo(
            id = "y6120QOlsfU", 
            title = "Gaming with Meowbah: Kawaii Edition",
            description = "Playing some super cute and fun games today! Let's see if I can win.",
            thumbnailUrl = "https://i.ytimg.com/vi/y6120QOlsfU/hqdefault.jpg",
            publishedAt = "2023-01-08T12:00:00Z" // Example date
        ),
        WidgetCompatibleVideo(
            id = "3JZ_D3ELwOQ", 
            title = "Meowbah Reacts to Fan Art",
            description = "You guys sent in so much amazing art! Let's take a look together!",
            thumbnailUrl = "https://i.ytimg.com/vi/3JZ_D3ELwOQ/hqdefault.jpg",
            publishedAt = "2023-01-15T12:00:00Z" // Example date
        ),
        WidgetCompatibleVideo(
            id = "L_LUpnjgPso", 
            title = "A Day in the Life of Meowbah",
            description = "What does a VTuber do all day? Come find out! Streaming, gaming, and more.",
            thumbnailUrl = "https://i.ytimg.com/vi/L_LUpnjgPso/hqdefault.jpg",
            publishedAt = "2023-01-22T12:00:00Z" // Example date
        ),
        WidgetCompatibleVideo(
            id = "kJQP7kiw5Fk", 
            title = "Meowbah's Q&A Special!",
            description = "Answering all your burning questions! Thanks for submitting them!",
            thumbnailUrl = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
            publishedAt = "2023-01-29T12:00:00Z" // Example date
        )
    )
}
