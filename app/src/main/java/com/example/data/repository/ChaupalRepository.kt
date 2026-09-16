package com.example.data.repository

import com.example.data.dao.ChaupalDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class ChaupalRepository(
    private val dao: ChaupalDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    // Current Active User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentProfile = MutableStateFlow<ProfileEntity?>(null)
    val currentProfile: StateFlow<ProfileEntity?> = _currentProfile.asStateFlow()

    init {
        externalScope.launch {
            checkAndSeedDemoData()
            // Auto login as default user (Aarav Sharma)
            val defaultUser = dao.getUserByUsername("aarav_sharma")
            if (defaultUser != null) {
                _currentUser.value = defaultUser
                _currentProfile.value = dao.getProfileByUserId(defaultUser.id)
            }
        }
    }

    // --- Authentication ---
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun login(identifier: String, password: String):Result<UserEntity> {
        val user = if (identifier.contains("@")) {
            dao.getUserByEmail(identifier.trim().lowercase())
        } else {
            dao.getUserByUsername(identifier.trim().lowercase())
        } ?: return Result.failure(Exception("User not found with provided credentials"))

        if (user.isBanned) {
            return Result.failure(Exception("Your account has been suspended by administration."))
        }

        val inputHash = hashPassword(password)
        if (user.passwordHash != inputHash) {
            return Result.failure(Exception("Incorrect password"))
        }

        _currentUser.value = user
        _currentProfile.value = dao.getProfileByUserId(user.id)
        return Result.success(user)
    }

    suspend fun signUp(
        username: String,
        email: String,
        fullName: String,
        password: String
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase().replace(" ", "_")
        val cleanEmail = email.trim().lowercase()

        if (cleanUsername.length < 3) {
            return Result.failure(Exception("Username must be at least 3 characters"))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(Exception("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters"))
        }

        if (dao.getUserByUsername(cleanUsername) != null) {
            return Result.failure(Exception("Username @$cleanUsername is already taken"))
        }
        if (dao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(Exception("An account with email $cleanEmail already exists"))
        }

        val userId = UUID.randomUUID().toString()
        val user = UserEntity(
            id = userId,
            username = cleanUsername,
            email = cleanEmail,
            passwordHash = hashPassword(password),
            isAdmin = false
        )
        val profile = ProfileEntity(
            userId = userId,
            fullName = fullName.ifBlank { cleanUsername.replaceFirstChar { it.uppercase() } },
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&q=80",
            coverUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1000&q=80",
            bio = "Hello Chaupal! Excited to connect with everyone here ✨",
            location = "New Delhi, India",
            website = "https://chaupal.social"
        )

        dao.insertUser(user)
        dao.insertProfile(profile)
        _currentUser.value = user
        _currentProfile.value = profile
        return Result.success(user)
    }

    suspend fun resetPassword(email: String, newPass: String): Result<Unit> {
        val user = dao.getUserByEmail(email.trim().lowercase())
            ?: return Result.failure(Exception("No account found with this email"))
        val updated = user.copy(passwordHash = hashPassword(newPass))
        dao.updateUser(updated)
        return Result.success(Unit)
    }

    fun logout() {
        _currentUser.value = null
        _currentProfile.value = null
    }

    // Switch account for demo purposes
    suspend fun switchDemoUser(username: String) {
        val user = dao.getUserByUsername(username)
        if (user != null) {
            _currentUser.value = user
            _currentProfile.value = dao.getProfileByUserId(user.id)
        }
    }

    // --- Feeds & Posts ---
    fun getAllPostsStream(): Flow<List<PostItem>> {
        return combine(
            dao.getAllPostsFlow(),
            currentUser
        ) { posts, user ->
            posts to user
        }.map { (posts, user) ->
            posts.mapNotNull { post ->
                val author = dao.getUserById(post.authorId) ?: return@mapNotNull null
                val authorProfile = dao.getProfileByUserId(post.authorId) ?: ProfileEntity(
                    userId = author.id,
                    fullName = author.username,
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&q=80",
                    coverUrl = "",
                    bio = "",
                    location = "",
                    website = ""
                )
                val isLiked = if (user != null) dao.getLike(user.id, post.id) != null else false
                val isSaved = if (user != null) dao.getBookmark(user.id, post.id) != null else false
                val tags = dao.getTagsForPost(post.id)
                PostItem(
                    post = post,
                    author = author,
                    authorProfile = authorProfile,
                    isLikedByMe = isLiked,
                    isSavedByMe = isSaved,
                    hashtags = tags
                )
            }
        }
    }

    suspend fun createPost(
        content: String,
        imageUrl: String? = null,
        videoUrl: String? = null,
        mediaType: String = "TEXT"
    ): Result<PostEntity> {
        val user = _currentUser.value ?: return Result.failure(Exception("Please log in first"))
        if (content.isBlank() && imageUrl.isNullOrBlank() && videoUrl.isNullOrBlank()) {
            return Result.failure(Exception("Post content or media cannot be empty"))
        }

        val postId = UUID.randomUUID().toString()
        val post = PostEntity(
            id = postId,
            authorId = user.id,
            content = content,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            mediaType = if (!videoUrl.isNullOrBlank()) "VIDEO" else if (!imageUrl.isNullOrBlank()) "IMAGE" else "TEXT"
        )
        dao.insertPost(post)

        // Increment user's post count
        val profile = dao.getProfileByUserId(user.id)
        if (profile != null) {
            dao.updateProfile(profile.copy(postsCount = profile.postsCount + 1))
            _currentProfile.value = profile.copy(postsCount = profile.postsCount + 1)
        }

        // Parse and store hashtags (#tag)
        val hashtagRegex = Regex("#[A-Za-z0-9_]+")
        val matches = hashtagRegex.findAll(content).map { it.value.lowercase() }.toSet()
        for (tag in matches) {
            dao.insertHashtag(HashtagEntity(tag = tag, postCount = 1, isTrending = true))
            dao.insertPostHashtag(PostHashtagEntity(id = UUID.randomUUID().toString(), postId = postId, tag = tag))
        }

        return Result.success(post)
    }

    suspend fun editPost(postId: String, newContent: String): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val post = dao.getPostById(postId) ?: return Result.failure(Exception("Post not found"))
        if (post.authorId != user.id && !user.isAdmin) {
            return Result.failure(Exception("Unauthorized: you can only edit your own posts"))
        }
        val updated = post.copy(
            content = newContent,
            isEdited = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.updatePost(updated)
        return Result.success(Unit)
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val post = dao.getPostById(postId) ?: return Result.failure(Exception("Post not found"))
        if (post.authorId != user.id && !user.isAdmin) {
            return Result.failure(Exception("Unauthorized: you can only delete your own posts"))
        }
        dao.deletePostById(postId)

        val profile = dao.getProfileByUserId(post.authorId)
        if (profile != null && profile.postsCount > 0) {
            dao.updateProfile(profile.copy(postsCount = profile.postsCount - 1))
            if (user.id == post.authorId) {
                _currentProfile.value = profile.copy(postsCount = profile.postsCount - 1)
            }
        }
        return Result.success(Unit)
    }

    suspend fun toggleLike(postId: String): Boolean {
        val user = _currentUser.value ?: return false
        val existing = dao.getLike(user.id, postId)
        val post = dao.getPostById(postId) ?: return false

        return if (existing != null) {
            dao.deleteLike(user.id, postId)
            dao.updatePostLikeCount(postId, -1)
            false
        } else {
            val like = LikeEntity(id = UUID.randomUUID().toString(), userId = user.id, postId = postId)
            dao.insertLike(like)
            dao.updatePostLikeCount(postId, 1)

            // Send notification to author
            if (post.authorId != user.id) {
                dao.insertNotification(
                    NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        recipientId = post.authorId,
                        senderId = user.id,
                        type = "LIKE",
                        postId = postId,
                        content = "liked your post"
                    )
                )
            }
            true
        }
    }

    suspend fun toggleBookmark(postId: String): Boolean {
        val user = _currentUser.value ?: return false
        val existing = dao.getBookmark(user.id, postId)
        return if (existing != null) {
            dao.deleteBookmark(user.id, postId)
            false
        } else {
            dao.insertBookmark(
                BookmarkEntity(id = UUID.randomUUID().toString(), userId = user.id, postId = postId)
            )
            true
        }
    }

    suspend fun sharePost(postId: String) {
        val user = _currentUser.value
        val post = dao.getPostById(postId) ?: return
        dao.updatePost(post.copy(shareCount = post.shareCount + 1))
        if (user != null && post.authorId != user.id) {
            dao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    recipientId = post.authorId,
                    senderId = user.id,
                    type = "SHARE",
                    postId = postId,
                    content = "shared your post"
                )
            )
        }
    }

    // --- Comments & Replies ---
    fun getCommentsStream(postId: String): Flow<List<CommentItem>> {
        return combine(
            dao.getCommentsForPostFlow(postId),
            currentUser
        ) { comments, user ->
            val topLevel = comments.filter { it.parentCommentId == null }
            val repliesMap = comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId }

            topLevel.mapNotNull { comment ->
                val author = dao.getUserById(comment.authorId) ?: return@mapNotNull null
                val profile = dao.getProfileByUserId(comment.authorId) ?: ProfileEntity(
                    userId = author.id, fullName = author.username, avatarUrl = "", coverUrl = "", bio = "", location = "", website = ""
                )
                val isLiked = if (user != null) dao.getCommentLike(user.id, comment.id) != null else false

                val replyItems = repliesMap[comment.id].orEmpty().mapNotNull { rep ->
                    val rAuthor = dao.getUserById(rep.authorId) ?: return@mapNotNull null
                    val rProfile = dao.getProfileByUserId(rep.authorId) ?: ProfileEntity(
                        userId = rAuthor.id, fullName = rAuthor.username, avatarUrl = "", coverUrl = "", bio = "", location = "", website = ""
                    )
                    val rLiked = if (user != null) dao.getCommentLike(user.id, rep.id) != null else false
                    CommentItem(comment = rep, author = rAuthor, authorProfile = rProfile, isLikedByMe = rLiked)
                }

                CommentItem(
                    comment = comment,
                    author = author,
                    authorProfile = profile,
                    isLikedByMe = isLiked,
                    replies = replyItems
                )
            }
        }
    }

    suspend fun addComment(postId: String, content: String, parentCommentId: String? = null): Result<CommentEntity> {
        val user = _currentUser.value ?: return Result.failure(Exception("Please log in to comment"))
        if (content.isBlank()) return Result.failure(Exception("Comment text cannot be empty"))

        val commentId = UUID.randomUUID().toString()
        val comment = CommentEntity(
            id = commentId,
            postId = postId,
            authorId = user.id,
            parentCommentId = parentCommentId,
            content = content
        )
        dao.insertComment(comment)
        dao.updatePostCommentCount(postId, 1)

        val post = dao.getPostById(postId)
        if (post != null && post.authorId != user.id) {
            dao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    recipientId = post.authorId,
                    senderId = user.id,
                    type = if (parentCommentId != null) "REPLY" else "COMMENT",
                    postId = postId,
                    commentId = commentId,
                    content = if (parentCommentId != null) "replied: \"$content\"" else "commented: \"$content\""
                )
            )
        }

        return Result.success(comment)
    }

    suspend fun deleteComment(commentId: String, postId: String): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        dao.deleteComment(commentId)
        dao.updatePostCommentCount(postId, -1)
        return Result.success(Unit)
    }

    suspend fun toggleCommentLike(commentId: String): Boolean {
        val user = _currentUser.value ?: return false
        val existing = dao.getCommentLike(user.id, commentId)
        return if (existing != null) {
            dao.deleteCommentLike(user.id, commentId)
            dao.updateCommentLikeCount(commentId, -1)
            false
        } else {
            dao.insertCommentLike(
                CommentLikeEntity(id = UUID.randomUUID().toString(), userId = user.id, commentId = commentId)
            )
            dao.updateCommentLikeCount(commentId, 1)
            true
        }
    }

    // --- Follow System ---
    suspend fun toggleFollow(targetUserId: String): Boolean {
        val user = _currentUser.value ?: return false
        if (user.id == targetUserId) return false

        val existing = dao.getFollowerRecord(followerId = user.id, followingId = targetUserId)
        val myProfile = dao.getProfileByUserId(user.id)
        val targetProfile = dao.getProfileByUserId(targetUserId)

        return if (existing != null) {
            dao.deleteFollower(user.id, targetUserId)
            if (myProfile != null) {
                dao.updateProfile(myProfile.copy(followingCount = maxOf(0, myProfile.followingCount - 1)))
                _currentProfile.value = myProfile.copy(followingCount = maxOf(0, myProfile.followingCount - 1))
            }
            if (targetProfile != null) {
                dao.updateProfile(targetProfile.copy(followersCount = maxOf(0, targetProfile.followersCount - 1)))
            }
            false
        } else {
            dao.insertFollower(
                FollowerEntity(id = UUID.randomUUID().toString(), followerId = user.id, followingId = targetUserId)
            )
            if (myProfile != null) {
                dao.updateProfile(myProfile.copy(followingCount = myProfile.followingCount + 1))
                _currentProfile.value = myProfile.copy(followingCount = myProfile.followingCount + 1)
            }
            if (targetProfile != null) {
                dao.updateProfile(targetProfile.copy(followersCount = targetProfile.followersCount + 1))
            }
            // Send notification
            dao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    recipientId = targetUserId,
                    senderId = user.id,
                    type = "FOLLOW",
                    content = "started following you"
                )
            )
            true
        }
    }

    suspend fun isFollowing(targetUserId: String): Boolean {
        val user = _currentUser.value ?: return false
        return dao.getFollowerRecord(user.id, targetUserId) != null
    }

    fun getSuggestedUsersStream(): Flow<List<UserSummary>> {
        return combine(
            dao.getAllUsersFlow(),
            currentUser
        ) { users, me ->
            users.filter { it.id != me?.id && !it.isBanned }
                .mapNotNull { u ->
                    val prof = dao.getProfileByUserId(u.id) ?: return@mapNotNull null
                    val following = if (me != null) dao.getFollowerRecord(me.id, u.id) != null else false
                    UserSummary(user = u, profile = prof, isFollowedByMe = following)
                }
        }
    }

    // --- Direct Messaging ---
    fun getConversationsStream(): Flow<List<ConversationSummary>> {
        val user = _currentUser.value
        if (user == null) return flowOf(emptyList())

        return dao.getConversationsFlow(user.id).map { convs ->
            convs.mapNotNull { conv ->
                val otherId = if (conv.user1Id == user.id) conv.user2Id else conv.user1Id
                val otherUser = dao.getUserById(otherId) ?: return@mapNotNull null
                val otherProfile = dao.getProfileByUserId(otherId) ?: return@mapNotNull null
                val unread = if (conv.user1Id == user.id) conv.unreadCountUser1 else conv.unreadCountUser2
                ConversationSummary(
                    conversation = conv,
                    otherUser = otherUser,
                    otherProfile = otherProfile,
                    unreadCount = unread
                )
            }
        }
    }

    fun getMessagesStream(conversationId: String): Flow<List<MessageEntity>> {
        return dao.getMessagesFlow(conversationId)
    }

    suspend fun getOrCreateConversation(otherUserId: String): ConversationEntity {
        val user = _currentUser.value ?: throw Exception("Not logged in")
        val existing = dao.findConversationBetween(user.id, otherUserId)
        if (existing != null) return existing

        val newConv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            user1Id = user.id,
            user2Id = otherUserId,
            lastMessageText = "Started a conversation",
            lastMessageTime = System.currentTimeMillis()
        )
        dao.insertConversation(newConv)
        return newConv
    }

    suspend fun sendMessage(conversationId: String, text: String, imageUrl: String? = null): Result<MessageEntity> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        if (text.isBlank() && imageUrl.isNullOrBlank()) return Result.failure(Exception("Message cannot be empty"))

        val conv = dao.getConversationsFlow(user.id).firstOrNull()?.find { it.id == conversationId }
            ?: return Result.failure(Exception("Conversation not found"))

        val receiverId = if (conv.user1Id == user.id) conv.user2Id else conv.user1Id
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = user.id,
            receiverId = receiverId,
            text = text,
            imageUrl = imageUrl
        )
        dao.insertMessage(msg)

        val updatedConv = conv.copy(
            lastMessageText = text.ifBlank { "Sent an image" },
            lastMessageTime = System.currentTimeMillis(),
            unreadCountUser1 = if (conv.user1Id == receiverId) conv.unreadCountUser1 + 1 else conv.unreadCountUser1,
            unreadCountUser2 = if (conv.user2Id == receiverId) conv.unreadCountUser2 + 1 else conv.unreadCountUser2
        )
        dao.updateConversation(updatedConv)
        return Result.success(msg)
    }

    suspend fun deleteMessage(messageId: String) {
        dao.deleteMessage(messageId)
    }

    // --- Notifications ---
    fun getNotificationsStream(): Flow<List<NotificationItem>> {
        val user = _currentUser.value ?: return flowOf(emptyList())
        return dao.getNotificationsFlow(user.id).map { list ->
            list.mapNotNull { notif ->
                val sender = dao.getUserById(notif.senderId) ?: return@mapNotNull null
                val profile = dao.getProfileByUserId(notif.senderId) ?: return@mapNotNull null
                NotificationItem(notification = notif, sender = sender, senderProfile = profile)
            }
        }
    }

    fun getUnreadNotificationsCountStream(): Flow<Int> {
        val user = _currentUser.value ?: return flowOf(0)
        return dao.getUnreadNotificationsCountFlow(user.id)
    }

    suspend fun markNotificationAsRead(id: String) = dao.markNotificationAsRead(id)
    suspend fun markAllNotificationsAsRead() {
        _currentUser.value?.let { dao.markAllNotificationsAsRead(it.id) }
    }

    // --- Profile & User Management ---
    suspend fun getUserProfile(userId: String): Pair<UserEntity?, ProfileEntity?> {
        val u = dao.getUserById(userId)
        val p = dao.getProfileByUserId(userId)
        return u to p
    }

    suspend fun updateProfile(
        fullName: String,
        bio: String,
        location: String,
        website: String,
        avatarUrl: String,
        coverUrl: String,
        isPrivate: Boolean
    ): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val existing = dao.getProfileByUserId(user.id) ?: return Result.failure(Exception("Profile not found"))
        val updated = existing.copy(
            fullName = fullName,
            bio = bio,
            location = location,
            website = website,
            avatarUrl = avatarUrl,
            coverUrl = coverUrl,
            isPrivate = isPrivate,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProfile(updated)
        _currentProfile.value = updated
        return Result.success(Unit)
    }

    // --- Search & Hashtags ---
    fun getTrendingHashtagsStream(): Flow<List<HashtagEntity>> = dao.getTrendingHashtagsFlow()

    suspend fun search(query: String): SearchResult {
        if (query.isBlank()) return SearchResult()
        val q = query.trim().lowercase()

        val allUsers = dao.getAllUsers()
        val allProfiles = dao.getAllProfiles().associateBy { it.userId }
        val matchedUsers = allUsers.filter {
            it.username.contains(q) || allProfiles[it.id]?.fullName?.lowercase()?.contains(q) == true
        }.mapNotNull { u ->
            val prof = allProfiles[u.id] ?: return@mapNotNull null
            val isFollowed = _currentUser.value?.let { me -> dao.getFollowerRecord(me.id, u.id) != null } ?: false
            UserSummary(user = u, profile = prof, isFollowedByMe = isFollowed)
        }

        val allPosts = dao.getAllPostsFlow().firstOrNull() ?: emptyList()
        val matchedPosts = allPosts.filter {
            it.content.lowercase().contains(q)
        }.mapNotNull { post ->
            val author = dao.getUserById(post.authorId) ?: return@mapNotNull null
            val authorProfile = dao.getProfileByUserId(post.authorId) ?: return@mapNotNull null
            val isLiked = _currentUser.value?.let { dao.getLike(it.id, post.id) != null } ?: false
            val isSaved = _currentUser.value?.let { dao.getBookmark(it.id, post.id) != null } ?: false
            PostItem(
                post = post,
                author = author,
                authorProfile = authorProfile,
                isLikedByMe = isLiked,
                isSavedByMe = isSaved,
                hashtags = dao.getTagsForPost(post.id)
            )
        }

        val allTags = dao.getAllHashtagsFlow().firstOrNull() ?: emptyList()
        val matchedTags = allTags.filter { it.tag.contains(q) }

        return SearchResult(
            users = matchedUsers,
            posts = matchedPosts,
            hashtags = matchedTags
        )
    }

    suspend fun getPostsForHashtag(tag: String): List<PostItem> {
        val postIds = dao.getPostIdsForTag(tag)
        return postIds.mapNotNull { id ->
            val post = dao.getPostById(id) ?: return@mapNotNull null
            val author = dao.getUserById(post.authorId) ?: return@mapNotNull null
            val authorProfile = dao.getProfileByUserId(post.authorId) ?: return@mapNotNull null
            val isLiked = _currentUser.value?.let { dao.getLike(it.id, post.id) != null } ?: false
            val isSaved = _currentUser.value?.let { dao.getBookmark(it.id, post.id) != null } ?: false
            PostItem(
                post = post,
                author = author,
                authorProfile = authorProfile,
                isLikedByMe = isLiked,
                isSavedByMe = isSaved,
                hashtags = dao.getTagsForPost(post.id)
            )
        }
    }

    // --- Admin Operations ---
    suspend fun getAdminMetrics(): AdminMetrics {
        val users = dao.getAllUsers()
        val posts = dao.getAllPostsFlow().firstOrNull() ?: emptyList()
        val comments = dao.getAllComments()
        val likes = dao.getAllLikes()
        val reports = dao.getAllReportsFlow().firstOrNull() ?: emptyList()

        return AdminMetrics(
            totalUsers = users.size,
            activeUsers = users.count { !it.isBanned },
            totalPosts = posts.size,
            totalComments = comments.size,
            totalLikes = likes.size,
            pendingReportsCount = reports.count { it.status == "PENDING" }
        )
    }

    fun getAllReportsStream(): Flow<List<ReportEntity>> = dao.getAllReportsFlow()

    suspend fun reportContent(targetType: String, targetId: String, reason: String): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(Exception("Please log in to report"))
        dao.insertReport(
            ReportEntity(
                id = UUID.randomUUID().toString(),
                reporterId = user.id,
                targetType = targetType,
                targetId = targetId,
                reason = reason
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleBanUser(userId: String): Boolean {
        val user = dao.getUserById(userId) ?: return false
        val newStatus = !user.isBanned
        dao.setUserBanned(userId, newStatus)
        return newStatus
    }

    suspend fun updateReportStatus(reportId: String, status: String) {
        dao.updateReportStatus(reportId, status)
    }

    // --- Blocked Users ---
    fun getBlockedUsersStream(): Flow<List<UserSummary>> {
        val user = _currentUser.value ?: return flowOf(emptyList())
        return dao.getBlockedUsersFlow(user.id).map { list ->
            list.mapNotNull { b ->
                val u = dao.getUserById(b.blockedUserId) ?: return@mapNotNull null
                val p = dao.getProfileByUserId(b.blockedUserId) ?: return@mapNotNull null
                UserSummary(user = u, profile = p, isFollowedByMe = false)
            }
        }
    }

    suspend fun blockUser(targetUserId: String) {
        val user = _currentUser.value ?: return
        dao.insertBlockedUser(
            BlockedUserEntity(id = UUID.randomUUID().toString(), blockerId = user.id, blockedUserId = targetUserId)
        )
    }

    suspend fun unblockUser(targetUserId: String) {
        val user = _currentUser.value ?: return
        dao.deleteBlockedUser(user.id, targetUserId)
    }

    // --- Demo Seed Data ---
    private suspend fun checkAndSeedDemoData() {
        if (dao.getAllUsers().isNotEmpty()) return

        // 1. Admin User
        val admin = UserEntity(
            id = "user_admin",
            username = "chaupal_admin",
            email = "admin@chaupal.social",
            passwordHash = hashPassword("admin123"),
            isAdmin = true
        )
        val adminProfile = ProfileEntity(
            userId = admin.id,
            fullName = "Chaupal Community Admin",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
            coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000&q=80",
            bio = "Official Chaupal Platform Team. Building an open, inclusive digital public square.",
            location = "Bengaluru, India",
            website = "https://chaupal.social",
            followersCount = 1420,
            followingCount = 45,
            postsCount = 3
        )
        dao.insertUser(admin)
        dao.insertProfile(adminProfile)

        // 2. Primary Demo User (Aarav Sharma)
        val aarav = UserEntity(
            id = "user_aarav",
            username = "aarav_sharma",
            email = "aarav@gmail.com",
            passwordHash = hashPassword("password123"),
            isAdmin = false
        )
        val aaravProfile = ProfileEntity(
            userId = aarav.id,
            fullName = "Aarav Sharma",
            avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&q=80",
            coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1000&q=80",
            bio = "Full-stack builder & open source advocate 💻 | Chai connoisseur ☕ | Exploring indie tech & photography #buildinpublic",
            location = "Jaipur, India",
            website = "https://aarav.dev",
            followersCount = 582,
            followingCount = 194,
            postsCount = 4
        )
        dao.insertUser(aarav)
        dao.insertProfile(aaravProfile)

        // 3. Priya Patel (Designer & Storyteller)
        val priya = UserEntity(
            id = "user_priya",
            username = "priya_designs",
            email = "priya@designs.io",
            passwordHash = hashPassword("password123")
        )
        val priyaProfile = ProfileEntity(
            userId = priya.id,
            fullName = "Priya Patel",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&q=80",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000&q=80",
            bio = "UI/UX & Brand Designer ✨ Creating calm digital experiences. Crafting typography & design tokens.",
            location = "Mumbai, India",
            website = "https://priyapatel.design",
            followersCount = 1240,
            followingCount = 310,
            postsCount = 5
        )
        dao.insertUser(priya)
        dao.insertProfile(priyaProfile)

        // 4. Kabir Khan (Traveler & Photographer)
        val kabir = UserEntity(
            id = "user_kabir",
            username = "kabir_lens",
            email = "kabir@lens.com",
            passwordHash = hashPassword("password123")
        )
        val kabirProfile = ProfileEntity(
            userId = kabir.id,
            fullName = "Kabir Khan",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
            coverUrl = "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1000&q=80",
            bio = "Capturing the heritage, colors, and faces of rural and modern India 📸 High altitude trekker 🏔️",
            location = "Manali, HP",
            website = "https://kabirlens.photo",
            followersCount = 890,
            followingCount = 142,
            postsCount = 6
        )
        dao.insertUser(kabir)
        dao.insertProfile(kabirProfile)

        // 5. Ananya Roy (Tech Journalist & AI researcher)
        val ananya = UserEntity(
            id = "user_ananya",
            username = "ananya_ai",
            email = "ananya@techdispatch.in",
            passwordHash = hashPassword("password123")
        )
        val ananyaProfile = ProfileEntity(
            userId = ananya.id,
            fullName = "Ananya Roy",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&q=80",
            coverUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=1000&q=80",
            bio = "Deep dive into on-device AI, ethics, and future of social networking. Writing for The Indian Tech Frontier.",
            location = "New Delhi, India",
            website = "https://techfrontier.in",
            followersCount = 2100,
            followingCount = 420,
            postsCount = 3
        )
        dao.insertUser(ananya)
        dao.insertProfile(ananyaProfile)

        // Initial Followers
        dao.insertFollower(FollowerEntity("f1", aarav.id, priya.id))
        dao.insertFollower(FollowerEntity("f2", aarav.id, kabir.id))
        dao.insertFollower(FollowerEntity("f3", priya.id, aarav.id))
        dao.insertFollower(FollowerEntity("f4", kabir.id, aarav.id))
        dao.insertFollower(FollowerEntity("f5", ananya.id, aarav.id))

        // Initial Hashtags
        val tags = listOf(
            HashtagEntity("#chaupal", 12, true),
            HashtagEntity("#buildinpublic", 8, true),
            HashtagEntity("#india", 15, true),
            HashtagEntity("#photography", 6, true),
            HashtagEntity("#tech", 10, true),
            HashtagEntity("#design", 7, false)
        )
        tags.forEach { dao.insertHashtag(it) }

        // Initial Posts
        val post1 = PostEntity(
            id = "p1",
            authorId = priya.id,
            content = "Welcome everyone to Chaupal! 🌿 We created this space so discussions feel warm, organic, and truly like gathering under the community tree. What's one feature you love most in modern apps? #chaupal #design",
            imageUrl = "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&q=80",
            mediaType = "IMAGE",
            likeCount = 42,
            commentCount = 5,
            shareCount = 11,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 4
        )
        dao.insertPost(post1)
        dao.insertPostHashtag(PostHashtagEntity("ph1", "p1", "#chaupal"))
        dao.insertPostHashtag(PostHashtagEntity("ph2", "p1", "#design"))

        val post2 = PostEntity(
            id = "p2",
            authorId = kabir.id,
            content = "Morning sunrise captured at Spiti Valley. The silence of the Himalayan peaks teaches patience like nothing else. 🌄 Keep exploring! #photography #india",
            imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&q=80",
            mediaType = "IMAGE",
            likeCount = 89,
            commentCount = 8,
            shareCount = 24,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 12
        )
        dao.insertPost(post2)
        dao.insertPostHashtag(PostHashtagEntity("ph3", "p2", "#photography"))
        dao.insertPostHashtag(PostHashtagEntity("ph4", "p2", "#india"))

        val post3 = PostEntity(
            id = "p3",
            authorId = aarav.id,
            content = "Just pushed the initial beta build of our Android client with Jetpack Compose and local offline Room sync! Everything loads at 120 FPS smoothly. What do you think of the new dark mode? #buildinpublic #tech #chaupal",
            imageUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80",
            mediaType = "IMAGE",
            likeCount = 56,
            commentCount = 6,
            shareCount = 7,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 20
        )
        dao.insertPost(post3)
        dao.insertPostHashtag(PostHashtagEntity("ph5", "p3", "#buildinpublic"))
        dao.insertPostHashtag(PostHashtagEntity("ph6", "p3", "#tech"))
        dao.insertPostHashtag(PostHashtagEntity("ph7", "p3", "#chaupal"))

        val post4 = PostEntity(
            id = "p4",
            authorId = ananya.id,
            content = "Exciting breakthrough in edge AI models running purely on-device without cloud latency or privacy compromises. The next era of social media will be decentralized and fast. Read my full analysis on techfrontier! #tech",
            mediaType = "TEXT",
            likeCount = 31,
            commentCount = 3,
            shareCount = 9,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 30
        )
        dao.insertPost(post4)
        dao.insertPostHashtag(PostHashtagEntity("ph8", "p4", "#tech"))

        // Comments for post 1
        dao.insertComment(
            CommentEntity(
                id = "c1",
                postId = "p1",
                authorId = aarav.id,
                content = "Loving the clean card UI and lightning fast responsiveness! Great launch Priya!",
                likeCount = 14,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 3
            )
        )
        dao.insertComment(
            CommentEntity(
                id = "c2",
                postId = "p1",
                authorId = kabir.id,
                content = "The name Chaupal brings back such nostalgic memories of community get-togethers.",
                likeCount = 9,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 2
            )
        )
        dao.insertComment(
            CommentEntity(
                id = "c3",
                postId = "p1",
                authorId = priya.id,
                parentCommentId = "c1",
                content = "Thanks Aarav! Can't wait for your offline mode updates next week 🙌",
                likeCount = 5,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 30
            )
        )

        // Seed initial notifications for Aarav
        dao.insertNotification(
            NotificationEntity(
                id = "n1",
                recipientId = aarav.id,
                senderId = priya.id,
                type = "LIKE",
                postId = "p3",
                content = "liked your post: \"Just pushed the initial beta build...\"",
                isRead = false,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 45
            )
        )
        dao.insertNotification(
            NotificationEntity(
                id = "n2",
                recipientId = aarav.id,
                senderId = kabir.id,
                type = "FOLLOW",
                content = "started following you",
                isRead = false,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 120
            )
        )
        dao.insertNotification(
            NotificationEntity(
                id = "n3",
                recipientId = aarav.id,
                senderId = ananya.id,
                type = "COMMENT",
                postId = "p3",
                content = "commented: \"Super clean architecture!\"",
                isRead = true,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 300
            )
        )

        // Seed initial conversation between Aarav & Priya
        val conv1 = ConversationEntity(
            id = "conv_aarav_priya",
            user1Id = aarav.id,
            user2Id = priya.id,
            lastMessageText = "Let me know when you've reviewed the design tokens!",
            lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 15,
            unreadCountUser1 = 1,
            unreadCountUser2 = 0
        )
        dao.insertConversation(conv1)
        dao.insertMessage(
            MessageEntity(
                id = "m1",
                conversationId = conv1.id,
                senderId = aarav.id,
                receiverId = priya.id,
                text = "Hey Priya! Did you get a chance to check the color palette in Figma?",
                createdAt = System.currentTimeMillis() - 1000 * 60 * 60
            )
        )
        dao.insertMessage(
            MessageEntity(
                id = "m2",
                conversationId = conv1.id,
                senderId = priya.id,
                receiverId = aarav.id,
                text = "Yes! The vibrant Indigo and Coral pairing is spot on for Chaupal.",
                createdAt = System.currentTimeMillis() - 1000 * 60 * 40
            )
        )
        dao.insertMessage(
            MessageEntity(
                id = "m3",
                conversationId = conv1.id,
                senderId = priya.id,
                receiverId = aarav.id,
                text = "Let me know when you've reviewed the design tokens!",
                createdAt = System.currentTimeMillis() - 1000 * 60 * 15
            )
        )

        // Seed initial conversation between Aarav & Kabir
        val conv2 = ConversationEntity(
            id = "conv_aarav_kabir",
            user1Id = aarav.id,
            user2Id = kabir.id,
            lastMessageText = "The Spiti Valley photo looks stunning brother!",
            lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 180,
            unreadCountUser1 = 0,
            unreadCountUser2 = 0
        )
        dao.insertConversation(conv2)
        dao.insertMessage(
            MessageEntity(
                id = "m4",
                conversationId = conv2.id,
                senderId = aarav.id,
                receiverId = kabir.id,
                text = "The Spiti Valley photo looks stunning brother!",
                createdAt = System.currentTimeMillis() - 1000 * 60 * 180
            )
        )
    }
}
