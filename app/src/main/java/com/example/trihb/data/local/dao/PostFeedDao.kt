package com.example.trihb.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.trihb.data.local.entity.PostFeedEntity
import kotlinx.coroutines.flow.Flow

/** CRUD de postagens da comunidade e incentivos (aplausos). */
@Dao
interface PostFeedDao {

    @Insert
    suspend fun inserirPost(post: PostFeedEntity): Long

    /** Mais recentes primeiro. */
    @Query("SELECT * FROM feed_posts ORDER BY id DESC")
    fun observarPosts(): Flow<List<PostFeedEntity>>

    /** Alterna incentivadoPorMim e incrementa/decrementa incentivosCount. */
    @Update
    suspend fun atualizarPost(post: PostFeedEntity)

    @Delete
    suspend fun deletarPost(post: PostFeedEntity)
}
