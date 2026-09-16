package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true), Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val isOnline: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["userId"], unique = true)]
)
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val avatarUrl: String,
    val coverUrl: String,
    val bio: String,
    val location: String,
    val website: String,
    val isPrivate: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "posts",
    indices = [Index(value = ["authorId"]), Index(value = ["createdAt"])]
)
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val content: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO"
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val isEdited: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "post_images",
    indices = [Index(value = ["postId"])]
)
data class PostImageEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val imageUrl: String,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "likes",
    indices = [Index(value = ["userId", "postId"], unique = true), Index(value = ["postId"])]
)
data class LikeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val postId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comments",
    indices = [Index(value = ["postId"]), Index(value = ["authorId"]), Index(value = ["parentCommentId"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val parentCommentId: String? = null,
    val content: String,
    val likeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comment_likes",
    indices = [Index(value = ["userId", "commentId"], unique = true)]
)
data class CommentLikeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val commentId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "followers",
    indices = [Index(value = ["followerId", "followingId"], unique = true), Index(value = ["followingId"])]
)
data class FollowerEntity(
    @PrimaryKey val id: String,
    val followerId: String,
    val followingId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["user1Id", "user2Id"])]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val user1Id: String,
    val user2Id: String,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val unreadCountUser1: Int = 0,
    val unreadCountUser2: Int = 0
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId"]), Index(value = ["senderId"]), Index(value = ["createdAt"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["recipientId"]), Index(value = ["createdAt"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val recipientId: String,
    val senderId: String,
    val type: String, // "FOLLOW", "LIKE", "COMMENT", "REPLY", "SHARE", "MENTION"
    val postId: String? = null,
    val commentId: String? = null,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hashtags")
data class HashtagEntity(
    @PrimaryKey val tag: String,
    val postCount: Int = 1,
    val isTrending: Boolean = false
)

@Entity(
    tableName = "post_hashtags",
    indices = [Index(value = ["postId"]), Index(value = ["tag"])]
)
data class PostHashtagEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val tag: String
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["userId", "postId"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val postId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reports",
    indices = [Index(value = ["targetType", "targetId"]), Index(value = ["status"])]
)
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val targetType: String, // "POST", "USER", "COMMENT"
    val targetId: String,
    val reason: String,
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "blocked_users",
    indices = [Index(value = ["blockerId", "blockedUserId"], unique = true)]
)
data class BlockedUserEntity(
    @PrimaryKey val id: String,
    val blockerId: String,
    val blockedUserId: String,
    val createdAt: Long = System.currentTimeMillis()
)
