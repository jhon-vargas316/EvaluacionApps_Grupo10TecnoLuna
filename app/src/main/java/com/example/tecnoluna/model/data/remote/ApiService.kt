package com.example.tecnoluna.model.data.remote

import com.example.tecnoluna.model.Post
import retrofit2.http.GET

interface ApiService {

    @GET("/posts")
    suspend fun getPosts(): List<Post>
}