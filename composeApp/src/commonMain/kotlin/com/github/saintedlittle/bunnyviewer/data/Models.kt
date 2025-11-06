package com.github.saintedlittle.bunnyviewer.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ChannelDto(
    val id: Long,
    val username: String? = null,
    val title: String
)


@Serializable
data class MediaDto(
    val id: Long,
    val type: String,
    val mime: String? = null,
    val size: Long? = null,
    @SerialName("file_path") val filePath: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null
)


@Serializable
data class PostDto(
    val id: Long,
    val channel: ChannelDto,
    @SerialName("tg_message_id") val tgMessageId: Long,
    val date: String,
    val text: String = "",
    val views: Long? = null,
    val forwards: Long? = null,
    val media: List<MediaDto> = emptyList()
)


@Serializable
data class UpdatesResponse(
    val items: List<PostDto> = emptyList()
)