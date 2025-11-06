package com.github.saintedlittle.bunnyviewer.data


import com.github.saintedlittle.bunnyviewer.Files

object LocalCache {
    private const val POSTS_FILE = "posts_cache.json"


    fun savePosts(posts: List<PostDto>) = Files.writeJson(POSTS_FILE, UpdatesResponse(posts))
    fun readPosts(): List<PostDto> = Files.readJson<UpdatesResponse>(POSTS_FILE)?.items ?: emptyList()
}