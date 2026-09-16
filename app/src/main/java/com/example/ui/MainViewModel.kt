package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ChaupalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppNavDestination {
    HOME,
    EXPLORE,
    MESSAGES,
    NOTIFICATIONS,
    PROFILE,
    SETTINGS,
    ADMIN
}

enum class FeedFilter {
    FOR_YOU,
    FOLLOWING,
    LATEST
}

enum class AuthModalState {
    NONE,
    LOGIN,
    SIGN_UP,
    FORGOT_PASSWORD,
    EDIT_PROFILE,
    CREATE_POST,
    REPORT_CONTENT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ChaupalRepository(db.chaupalDao(), viewModelScope)

    val currentUser = repository.currentUser
    val currentProfile = repository.currentProfile

    private val _currentNav = MutableStateFlow(AppNavDestination.HOME)
    val currentNav: StateFlow<AppNavDestination> = _currentNav.asStateFlow()

    private val _feedFilter = MutableStateFlow(FeedFilter.FOR_YOU)
    val feedFilter: StateFlow<FeedFilter> = _feedFilter.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _authModalState = MutableStateFlow(AuthModalState.NONE)
    val authModalState: StateFlow<AuthModalState> = _authModalState.asStateFlow()

    private val _modalTargetPostId = MutableStateFlow<String?>(null)
    val modalTargetPostId: StateFlow<String?> = _modalTargetPostId.asStateFlow()

    // Comment Sheet
    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeCommentPostId: StateFlow<String?> = _activeCommentPostId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeComments: StateFlow<List<CommentItem>> = _activeCommentPostId
        .flatMapLatest { postId ->
            if (postId == null) flowOf(emptyList())
            else repository.getCommentsStream(postId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All posts
    val allPosts: StateFlow<List<PostItem>> = repository.getAllPostsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered posts for home feed
    val feedPosts: StateFlow<List<PostItem>> = combine(
        allPosts,
        _feedFilter,
        currentUser
    ) { posts, filter, user ->
        when (filter) {
            FeedFilter.FOR_YOU -> posts
            FeedFilter.LATEST -> posts.sortedByDescending { it.post.createdAt }
            FeedFilter.FOLLOWING -> {
                // If user is logged in, show posts from users they follow or themselves
                posts.filter { it.post.authorId == user?.id || it.isLikedByMe }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trending hashtags
    val trendingHashtags: StateFlow<List<HashtagEntity>> = repository.getTrendingHashtagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Suggested users
    val suggestedUsers: StateFlow<List<UserSummary>> = repository.getSuggestedUsersStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<NotificationItem>> = repository.getNotificationsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.getUnreadNotificationsCountStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Direct Messaging
    val conversations: StateFlow<List<ConversationSummary>> = repository.getConversationsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId == null) flowOf(emptyList())
            else repository.getMessagesStream(convId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow(SearchResult())
    val searchResult: StateFlow<SearchResult> = _searchResult.asStateFlow()

    // Hashtag Detail View
    private val _selectedHashtag = MutableStateFlow<String?>(null)
    val selectedHashtag: StateFlow<String?> = _selectedHashtag.asStateFlow()

    private val _hashtagPosts = MutableStateFlow<List<PostItem>>(emptyList())
    val hashtagPosts: StateFlow<List<PostItem>> = _hashtagPosts.asStateFlow()

    // User Profile Detail View (viewing other user's profile)
    private val _viewingUserId = MutableStateFlow<String?>(null)
    val viewingUserId: StateFlow<String?> = _viewingUserId.asStateFlow()

    private val _viewingUserData = MutableStateFlow<Pair<UserEntity?, ProfileEntity?>?>(null)
    val viewingUserData: StateFlow<Pair<UserEntity?, ProfileEntity?>?> = _viewingUserData.asStateFlow()

    // Admin Dashboard
    private val _adminMetrics = MutableStateFlow(AdminMetrics())
    val adminMetrics: StateFlow<AdminMetrics> = _adminMetrics.asStateFlow()

    val adminReports: StateFlow<List<ReportEntity>> = repository.getAllReportsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Blocked users
    val blockedUsers: StateFlow<List<UserSummary>> = repository.getBlockedUsersStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Messages
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    fun showSnack(message: String) {
        _snackMessage.value = message
    }

    fun clearSnack() {
        _snackMessage.value = null
    }

    fun setNav(nav: AppNavDestination) {
        _currentNav.value = nav
        if (nav == AppNavDestination.ADMIN) {
            refreshAdminMetrics()
        }
    }

    fun setFeedFilter(filter: FeedFilter) {
        _feedFilter.value = filter
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setModal(modal: AuthModalState, targetPostId: String? = null) {
        _authModalState.value = modal
        _modalTargetPostId.value = targetPostId
    }

    fun openComments(postId: String) {
        _activeCommentPostId.value = postId
    }

    fun closeComments() {
        _activeCommentPostId.value = null
    }

    // --- Authentication Actions ---
    fun login(id: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.login(id, pass)
            result.onSuccess {
                launch(Dispatchers.Main) {
                    setModal(AuthModalState.NONE)
                    showSnack("Welcome back, @${it.username}!")
                    onResult(true, null)
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Login failed")
                    onResult(false, err.message)
                }
            }
        }
    }

    fun signUp(username: String, email: String, fullName: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.signUp(username, email, fullName, pass)
            result.onSuccess {
                launch(Dispatchers.Main) {
                    setModal(AuthModalState.NONE)
                    showSnack("Account created! Welcome to Chaupal, @${it.username} 🎉")
                    onResult(true, null)
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Sign up failed")
                    onResult(false, err.message)
                }
            }
        }
    }

    fun resetPassword(email: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.resetPassword(email, newPass)
            result.onSuccess {
                launch(Dispatchers.Main) {
                    setModal(AuthModalState.LOGIN)
                    showSnack("Password reset successfully! Please login.")
                    onResult(true, null)
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Reset password failed")
                    onResult(false, err.message)
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        setNav(AppNavDestination.HOME)
        showSnack("Logged out of Chaupal")
    }

    fun switchDemoUser(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.switchDemoUser(username)
            launch(Dispatchers.Main) {
                showSnack("Switched active demo account to @$username")
            }
        }
    }

    // --- Post Actions ---
    fun createPost(content: String, imageUrl: String? = null, videoUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.createPost(content, imageUrl, videoUrl)
            res.onSuccess {
                launch(Dispatchers.Main) {
                    setModal(AuthModalState.NONE)
                    showSnack("Post published to Chaupal! 🚀")
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Error publishing post")
                }
            }
        }
    }

    fun editPost(postId: String, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.editPost(postId, newContent)
            res.onSuccess {
                launch(Dispatchers.Main) {
                    showSnack("Post updated successfully!")
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Failed to edit post")
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.deletePost(postId)
            res.onSuccess {
                launch(Dispatchers.Main) {
                    showSnack("Post deleted")
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleLike(postId)
        }
    }

    fun toggleBookmark(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = repository.toggleBookmark(postId)
            launch(Dispatchers.Main) {
                showSnack(if (saved) "Post saved to Bookmarks" else "Post removed from Bookmarks")
            }
        }
    }

    fun sharePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sharePost(postId)
            launch(Dispatchers.Main) {
                showSnack("Link copied & post shared! 🔗")
            }
        }
    }

    // --- Comments ---
    fun addComment(postId: String, content: String, parentId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.addComment(postId, content, parentId)
            res.onSuccess {
                launch(Dispatchers.Main) {
                    showSnack("Comment added!")
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Failed to comment")
                }
            }
        }
    }

    fun deleteComment(commentId: String, postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(commentId, postId)
        }
    }

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleCommentLike(commentId)
        }
    }

    // --- Follow System ---
    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowFollowing = repository.toggleFollow(targetUserId)
            launch(Dispatchers.Main) {
                showSnack(if (nowFollowing) "Followed user" else "Unfollowed user")
            }
        }
    }

    // --- Messaging ---
    fun openConversation(convId: String) {
        _activeConversationId.value = convId
        setNav(AppNavDestination.MESSAGES)
    }

    fun startConversationWith(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conv = repository.getOrCreateConversation(otherUserId)
                launch(Dispatchers.Main) {
                    _activeConversationId.value = conv.id
                    setNav(AppNavDestination.MESSAGES)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    showSnack(e.message ?: "Could not open conversation")
                }
            }
        }
    }

    fun closeConversation() {
        _activeConversationId.value = null
    }

    fun sendMessage(text: String, imageUrl: String? = null) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(convId, text, imageUrl)
        }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(msgId)
        }
    }

    // --- Notifications ---
    fun markAllNotificationsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllNotificationsAsRead()
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markNotificationAsRead(id)
        }
    }

    // --- Search & Hashtags ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.search(query)
            _searchResult.value = res
        }
    }

    fun selectHashtag(tag: String) {
        _selectedHashtag.value = tag
        viewModelScope.launch(Dispatchers.IO) {
            val posts = repository.getPostsForHashtag(tag)
            _hashtagPosts.value = posts
            launch(Dispatchers.Main) {
                setNav(AppNavDestination.EXPLORE)
            }
        }
    }

    fun clearSelectedHashtag() {
        _selectedHashtag.value = null
        _hashtagPosts.value = emptyList()
    }

    // --- User Profile Viewing ---
    fun openUserProfile(userId: String) {
        _viewingUserId.value = userId
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getUserProfile(userId)
            _viewingUserData.value = data
            launch(Dispatchers.Main) {
                setNav(AppNavDestination.PROFILE)
            }
        }
    }

    fun openMyProfile() {
        _viewingUserId.value = null
        _viewingUserData.value = null
        setNav(AppNavDestination.PROFILE)
    }

    fun updateProfile(
        fullName: String,
        bio: String,
        location: String,
        website: String,
        avatarUrl: String,
        coverUrl: String,
        isPrivate: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.updateProfile(fullName, bio, location, website, avatarUrl, coverUrl, isPrivate)
            res.onSuccess {
                launch(Dispatchers.Main) {
                    setModal(AuthModalState.NONE)
                    showSnack("Profile updated successfully!")
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Failed to update profile")
                }
            }
        }
    }

    // --- Admin Dashboard ---
    fun refreshAdminMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            val metrics = repository.getAdminMetrics()
            _adminMetrics.value = metrics
        }
    }

    fun toggleBanUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val banned = repository.toggleBanUser(userId)
            refreshAdminMetrics()
            launch(Dispatchers.Main) {
                showSnack(if (banned) "User has been banned" else "User ban lifted")
            }
        }
    }

    fun updateReportStatus(reportId: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReportStatus(reportId, status)
            refreshAdminMetrics()
            launch(Dispatchers.Main) {
                showSnack("Report marked as $status")
            }
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.reportContent(targetType, targetId, reason)
            res.onSuccess {
                launch(Dispatchers.Main) {
                    setModal(AuthModalState.NONE)
                    showSnack("Thank you. Report submitted to moderation team.")
                }
            }.onFailure { err ->
                launch(Dispatchers.Main) {
                    showSnack(err.message ?: "Failed to submit report")
                }
            }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.blockUser(userId)
            launch(Dispatchers.Main) {
                showSnack("User blocked")
            }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unblockUser(userId)
            launch(Dispatchers.Main) {
                showSnack("User unblocked")
            }
        }
    }
}
