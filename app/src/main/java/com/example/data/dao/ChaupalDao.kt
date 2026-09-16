package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChaupalDao {

    // --- Users & Profiles ---
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isBanned = :isBanned WHERE id = :userId")
    suspend fun setUserBanned(userId: String, isBanned: Boolean)

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfileByUserId(userId: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    fun getProfileFlow(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    // --- Posts ---
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getPostsByAuthorFlow(authorId: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("UPDATE posts SET likeCount = MAX(0, likeCount + :delta) WHERE id = :postId")
    suspend fun updatePostLikeCount(postId: String, delta: Int)

    @Query("UPDATE posts SET commentCount = MAX(0, commentCount + :delta) WHERE id = :postId")
    suspend fun updatePostCommentCount(postId: String, delta: Int)

    // --- Likes ---
    @Query("SELECT * FROM likes WHERE userId = :userId AND postId = :postId LIMIT 1")
    suspend fun getLike(userId: String, postId: String): LikeEntity?

    @Query("SELECT * FROM likes WHERE userId = :userId")
    fun getUserLikesFlow(userId: String): Flow<List<LikeEntity>>

    @Query("SELECT * FROM likes")
    suspend fun getAllLikes(): List<LikeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE userId = :userId AND postId = :postId")
    suspend fun deleteLike(userId: String, postId: String)

    // --- Comments ---
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun getCommentsForPostFlow(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments")
    suspend fun getAllComments(): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    @Query("UPDATE comments SET likeCount = MAX(0, likeCount + :delta) WHERE id = :commentId")
    suspend fun updateCommentLikeCount(commentId: String, delta: Int)

    // --- Comment Likes ---
    @Query("SELECT * FROM comment_likes WHERE userId = :userId AND commentId = :commentId LIMIT 1")
    suspend fun getCommentLike(userId: String, commentId: String): CommentLikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentLike(like: CommentLikeEntity)

    @Query("DELETE FROM comment_likes WHERE userId = :userId AND commentId = :commentId")
    suspend fun deleteCommentLike(userId: String, commentId: String)

    // --- Followers ---
    @Query("SELECT * FROM followers WHERE followerId = :followerId AND followingId = :followingId LIMIT 1")
    suspend fun getFollowerRecord(followerId: String, followingId: String): FollowerEntity?

    @Query("SELECT * FROM followers WHERE followerId = :followerId")
    fun getFollowingFlow(followerId: String): Flow<List<FollowerEntity>>

    @Query("SELECT * FROM followers WHERE followingId = :followingId")
    fun getFollowersFlow(followingId: String): Flow<List<FollowerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollower(follower: FollowerEntity)

    @Query("DELETE FROM followers WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollower(followerId: String, followingId: String)

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks WHERE userId = :userId AND postId = :postId LIMIT 1")
    suspend fun getBookmark(userId: String, postId: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookmarksFlow(userId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE userId = :userId AND postId = :postId")
    suspend fun deleteBookmark(userId: String, postId: String)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE recipientId = :recipientId ORDER BY createdAt DESC")
    fun getNotificationsFlow(recipientId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE recipientId = :recipientId AND isRead = 0")
    fun getUnreadNotificationsCountFlow(recipientId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientId = :recipientId")
    suspend fun markAllNotificationsAsRead(recipientId: String)

    // --- Conversations & Messages ---
    @Query("SELECT * FROM conversations WHERE user1Id = :userId OR user2Id = :userId ORDER BY lastMessageTime DESC")
    fun getConversationsFlow(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE (user1Id = :user1 AND user2Id = :user2) OR (user1Id = :user2 AND user2Id = :user1) LIMIT 1")
    suspend fun findConversationBetween(user1: String, user2: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    // --- Hashtags ---
    @Query("SELECT * FROM hashtags ORDER BY postCount DESC")
    fun getAllHashtagsFlow(): Flow<List<HashtagEntity>>

    @Query("SELECT * FROM hashtags WHERE isTrending = 1 ORDER BY postCount DESC")
    fun getTrendingHashtagsFlow(): Flow<List<HashtagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashtag(hashtag: HashtagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostHashtag(postHashtag: PostHashtagEntity)

    @Query("SELECT tag FROM post_hashtags WHERE postId = :postId")
    suspend fun getTagsForPost(postId: String): List<String>

    @Query("SELECT postId FROM post_hashtags WHERE tag = :tag")
    suspend fun getPostIdsForTag(tag: String): List<String>

    // --- Reports ---
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)

    // --- Blocked Users ---
    @Query("SELECT * FROM blocked_users WHERE blockerId = :blockerId")
    fun getBlockedUsersFlow(blockerId: String): Flow<List<BlockedUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(blocked: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE blockerId = :blockerId AND blockedUserId = :blockedUserId")
    suspend fun deleteBlockedUser(blockerId: String, blockedUserId: String)
}
