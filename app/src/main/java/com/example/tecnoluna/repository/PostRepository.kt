package com.example.tecnoluna.repository

import com.example.tecnoluna.model.Post
import com.example.tecnoluna.model.data.remote.RetrofitInstance

class PostRepository {

    suspend fun getPosts(): List<Post>{
        return RetrofitInstance.api.getPosts()
    }
}