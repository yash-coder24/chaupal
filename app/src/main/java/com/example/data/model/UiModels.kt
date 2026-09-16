package com.example.data.model

data class PostItem(
    val post: PostEntity,
    val author: UserEntity,
    val authorProfile: ProfileEntity,
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val images: List<PostImageEntity> = emptyList(),
    val hashtags: List<String> = emptyList()
)

data class CommentItem(
    val comment: CommentEntity,
    val author: UserEntity,
    val authorProfile: ProfileEntity,
    val isLikedByMe: Boolean = false,
    val replies: List<CommentItem> = emptyList()
)

data class UserSummary(
    val user: UserEntity,
    val profile: ProfileEntity,
    val isFollowedByMe: Boolean = false
)

data class ConversationSummary(
    val conversation: ConversationEntity,
    val otherUser: UserEntity,
    val otherProfile: ProfileEntity,
    val unreadCount: Int = 0
)

data class NotificationItem(
    val notification: NotificationEntity,
    val sender: UserEntity,
    val senderProfile: ProfileEntity
)

data class AdminMetrics(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalPosts: Int = 0,
    val totalComments: Int = 0,
    val totalLikes: Int = 0,
    val pendingReportsCount: Int = 0
)

data class SearchResult(
    val users: List<UserSummary> = emptyList(),
    val posts: List<PostItem> = emptyList(),
    val hashtags: List<HashtagEntity> = emptyList()
)
