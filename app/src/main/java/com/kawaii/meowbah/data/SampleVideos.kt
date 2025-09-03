package com.kawaii.meowbah.data

// A simplified data class for widget video items
data class WidgetCompatibleVideo(
    val id: String, // Video ID
    val title: String,
    val description: String
    // We can add thumbnail URL later if needed for the widget item, but RemoteViews are tricky with images
)

object SampleVideos {
    // This is a MOCK list. In a real scenario, you'd fetch this from a repository or database.
    val MOCK_VIDEO_LIST: List<WidgetCompatibleVideo> = listOf(
        WidgetCompatibleVideo(
            id = "dQw4w9WgXcQ", // Example Video ID
            title = "Meowbah's First Vlog!",
            description = "Welcome to my channel! Join me on my first adventure as a VTuber."
        ),
        WidgetCompatibleVideo(
            id = "y6120QOlsfU", // Example Video ID
            title = "Gaming with Meowbah: Kawaii Edition",
            description = "Playing some super cute and fun games today! Let's see if I can win."
        ),
        WidgetCompatibleVideo(
            id = "3JZ_D3ELwOQ", // Example Video ID
            title = "Meowbah Reacts to Fan Art",
            description = "You guys sent in so much amazing art! Let's take a look together!"
        ),
        WidgetCompatibleVideo(
            id = "L_LUpnjgPso", // Example Video ID
            title = "A Day in the Life of Meowbah",
            description = "What does a VTuber do all day? Come find out! Streaming, gaming, and more."
        ),
        WidgetCompatibleVideo(
            id = "kJQP7kiw5Fk", // Example Video ID
            title = "Meowbah's Q&A Special!",
            description = "Answering all your burning questions! Thanks for submitting them!"
        )
    )
}
