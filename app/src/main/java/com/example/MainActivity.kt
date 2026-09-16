package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PostItem
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.ChaupalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val isDarkMode by vm.isDarkMode.collectAsState()

            ChaupalTheme(darkTheme = isDarkMode) {
                ChaupalApp(vm = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaupalApp(vm: MainViewModel) {
    val currentUser by vm.currentUser.collectAsState()
    val currentProfile by vm.currentProfile.collectAsState()
    val currentNav by vm.currentNav.collectAsState()
    val feedFilter by vm.feedFilter.collectAsState()
    val isDarkMode by vm.isDarkMode.collectAsState()
    val authModalState by vm.authModalState.collectAsState()
    val modalTargetPostId by vm.modalTargetPostId.collectAsState()

    val feedPosts by vm.feedPosts.collectAsState()
    val allPosts by vm.allPosts.collectAsState()
    val trendingHashtags by vm.trendingHashtags.collectAsState()
    val suggestedUsers by vm.suggestedUsers.collectAsState()
    val notifications by vm.notifications.collectAsState()
    val unreadNotificationsCount by vm.unreadNotificationsCount.collectAsState()

    val conversations by vm.conversations.collectAsState()
    val activeConversationId by vm.activeConversationId.collectAsState()
    val activeMessages by vm.activeMessages.collectAsState()

    val searchQuery by vm.searchQuery.collectAsState()
    val searchResult by vm.searchResult.collectAsState()
    val selectedHashtag by vm.selectedHashtag.collectAsState()
    val hashtagPosts by vm.hashtagPosts.collectAsState()

    val viewingUserId by vm.viewingUserId.collectAsState()
    val viewingUserData by vm.viewingUserData.collectAsState()

    val adminMetrics by vm.adminMetrics.collectAsState()
    val adminReports by vm.adminReports.collectAsState()
    val blockedUsers by vm.blockedUsers.collectAsState()

    val activeCommentPostId by vm.activeCommentPostId.collectAsState()
    val activeComments by vm.activeComments.collectAsState()

    val snackMessage by vm.snackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearchActive by remember { mutableStateOf(false) }
    var editingPostItem by remember { mutableStateOf<PostItem?>(null) }
    var reportingPostId by remember { mutableStateOf<String?>(null) }
    var reportingUserId by remember { mutableStateOf<String?>(null) }

    // React to snackbar messages
    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            isSearchActive = false
                            vm.clearSelectedHashtag()
                            vm.setNav(AppNavDestination.HOME)
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chaupal_logo),
                            contentDescription = "Chaupal Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Chaupal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("topbar_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Dark/Light toggle
                    IconButton(
                        onClick = { vm.toggleDarkMode() },
                        modifier = Modifier.testTag("topbar_theme_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Direct Messages with unread badge
                    IconButton(
                        onClick = {
                            isSearchActive = false
                            vm.setNav(AppNavDestination.MESSAGES)
                        },
                        modifier = Modifier.testTag("topbar_messages_button")
                    ) {
                        val totalUnreadMsgs = conversations.sumOf { it.unreadCount }
                        BadgedBox(
                            badge = {
                                if (totalUnreadMsgs > 0) {
                                    Badge { Text("$totalUnreadMsgs") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentNav == AppNavDestination.MESSAGES) Icons.Filled.Mail else Icons.Outlined.Mail,
                                contentDescription = "Messages"
                            )
                        }
                    }

                    // User avatar / Login button
                    if (currentUser != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable {
                                    isSearchActive = false
                                    vm.openMyProfile()
                                }
                        ) {
                            UserAvatar(
                                avatarUrl = currentProfile?.avatarUrl,
                                displayName = currentProfile?.fullName ?: "Me",
                                size = 34.dp
                            )
                        }
                    } else {
                        Button(
                            onClick = { vm.setModal(AuthModalState.LOGIN) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(36.dp)
                        ) {
                            Text("Sign In", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentNav == AppNavDestination.HOME && !isSearchActive,
                    onClick = {
                        isSearchActive = false
                        vm.clearSelectedHashtag()
                        vm.setNav(AppNavDestination.HOME)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentNav == AppNavDestination.HOME && !isSearchActive) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Feed") },
                    modifier = Modifier.testTag("nav_item_home")
                )

                NavigationBarItem(
                    selected = currentNav == AppNavDestination.EXPLORE && !isSearchActive,
                    onClick = {
                        isSearchActive = false
                        vm.setNav(AppNavDestination.EXPLORE)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentNav == AppNavDestination.EXPLORE && !isSearchActive) Icons.Filled.Explore else Icons.Outlined.Explore,
                            contentDescription = "Explore"
                        )
                    },
                    label = { Text("Explore") },
                    modifier = Modifier.testTag("nav_item_explore")
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { vm.setModal(AuthModalState.CREATE_POST) },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create Post",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    label = { Text("Post") },
                    modifier = Modifier.testTag("nav_item_create")
                )

                NavigationBarItem(
                    selected = currentNav == AppNavDestination.NOTIFICATIONS && !isSearchActive,
                    onClick = {
                        isSearchActive = false
                        vm.setNav(AppNavDestination.NOTIFICATIONS)
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text("$unreadNotificationsCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentNav == AppNavDestination.NOTIFICATIONS && !isSearchActive) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    },
                    label = { Text("Alerts") },
                    modifier = Modifier.testTag("nav_item_notifications")
                )

                NavigationBarItem(
                    selected = currentNav == AppNavDestination.PROFILE && viewingUserId == null && !isSearchActive,
                    onClick = {
                        isSearchActive = false
                        vm.openMyProfile()
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentNav == AppNavDestination.PROFILE && viewingUserId == null && !isSearchActive) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_item_profile")
                )
            }
        },
        floatingActionButton = {
            if (currentNav == AppNavDestination.HOME && !isSearchActive) {
                FloatingActionButton(
                    onClick = { vm.setModal(AuthModalState.CREATE_POST) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("home_create_post_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearchActive) {
                SearchScreen(
                    query = searchQuery,
                    searchResult = searchResult,
                    onQueryChange = { vm.setSearchQuery(it) },
                    onUserClick = {
                        isSearchActive = false
                        vm.openUserProfile(it)
                    },
                    onTagClick = { tag ->
                        isSearchActive = false
                        vm.selectHashtag(tag)
                    },
                    onToggleFollow = { vm.toggleFollow(it) },
                    onLikeClick = { vm.toggleLike(it) },
                    onCommentClick = { vm.openComments(it) },
                    onShareClick = { vm.sharePost(it) },
                    onBookmarkClick = { vm.toggleBookmark(it) },
                    onEditClick = { editingPostItem = it },
                    onDeleteClick = { vm.deletePost(it) },
                    onReportClick = { reportingPostId = it },
                    currentUserId = currentUser?.id
                )
            } else {
                when (currentNav) {
                    AppNavDestination.HOME -> {
                        FeedScreen(
                            posts = feedPosts,
                            currentFilter = feedFilter,
                            currentUser = currentUser,
                            currentProfile = currentProfile,
                            suggestedUsers = suggestedUsers,
                            onFilterChange = { vm.setFeedFilter(it) },
                            onOpenCreatePost = { vm.setModal(AuthModalState.CREATE_POST) },
                            onUserClick = { vm.openUserProfile(it) },
                            onLikeClick = { vm.toggleLike(it) },
                            onCommentClick = { vm.openComments(it) },
                            onShareClick = { vm.sharePost(it) },
                            onBookmarkClick = { vm.toggleBookmark(it) },
                            onHashtagClick = { vm.selectHashtag(it) },
                            onEditClick = { editingPostItem = it },
                            onDeleteClick = { vm.deletePost(it) },
                            onReportClick = { reportingPostId = it },
                            onToggleFollow = { vm.toggleFollow(it) }
                        )
                    }
                    AppNavDestination.EXPLORE -> {
                        ExploreScreen(
                            trendingHashtags = trendingHashtags,
                            suggestedUsers = suggestedUsers,
                            allPosts = allPosts,
                            selectedHashtag = selectedHashtag,
                            hashtagPosts = hashtagPosts,
                            onSelectHashtag = { vm.selectHashtag(it) },
                            onClearHashtag = { vm.clearSelectedHashtag() },
                            onSearchClick = { isSearchActive = true },
                            onUserClick = { vm.openUserProfile(it) },
                            onToggleFollow = { vm.toggleFollow(it) },
                            onLikeClick = { vm.toggleLike(it) },
                            onCommentClick = { vm.openComments(it) },
                            onShareClick = { vm.sharePost(it) },
                            onBookmarkClick = { vm.toggleBookmark(it) },
                            onEditClick = { editingPostItem = it },
                            onDeleteClick = { vm.deletePost(it) },
                            onReportClick = { reportingPostId = it },
                            currentUserId = currentUser?.id
                        )
                    }
                    AppNavDestination.MESSAGES -> {
                        MessagesScreen(
                            conversations = conversations,
                            activeConversationId = activeConversationId,
                            messages = activeMessages,
                            currentUser = currentUser,
                            onSelectConversation = { vm.openConversation(it) },
                            onBackToList = { vm.closeConversation() },
                            onSendMessage = { vm.sendMessage(it) },
                            onDeleteMessage = { vm.deleteMessage(it) },
                            onUserClick = { vm.openUserProfile(it) }
                        )
                    }
                    AppNavDestination.NOTIFICATIONS -> {
                        NotificationsScreen(
                            notifications = notifications,
                            unreadCount = unreadNotificationsCount,
                            onMarkAllRead = { vm.markAllNotificationsRead() },
                            onNotificationClick = { item ->
                                vm.markNotificationRead(item.notification.id)
                                if (item.notification.postId != null) {
                                    vm.openComments(item.notification.postId)
                                } else {
                                    vm.openUserProfile(item.sender.id)
                                }
                            },
                            onUserClick = { vm.openUserProfile(it) }
                        )
                    }
                    AppNavDestination.PROFILE -> {
                        val isMyView = viewingUserId == null || viewingUserId == currentUser?.id
                        val displayUser = if (isMyView) currentUser else viewingUserData?.first
                        val displayProfile = if (isMyView) currentProfile else viewingUserData?.second

                        ProfileScreen(
                            user = displayUser,
                            profile = displayProfile,
                            currentUserId = currentUser?.id,
                            posts = allPosts,
                            isFollowing = false,
                            onToggleFollow = { displayUser?.let { vm.toggleFollow(it.id) } },
                            onStartMessage = { displayUser?.let { vm.startConversationWith(it.id) } },
                            onEditProfile = { vm.setModal(AuthModalState.EDIT_PROFILE) },
                            onOpenSettings = { vm.setNav(AppNavDestination.SETTINGS) },
                            onLikeClick = { vm.toggleLike(it) },
                            onCommentClick = { vm.openComments(it) },
                            onShareClick = { vm.sharePost(it) },
                            onBookmarkClick = { vm.toggleBookmark(it) },
                            onHashtagClick = { vm.selectHashtag(it) },
                            onEditPost = { editingPostItem = it },
                            onDeletePost = { vm.deletePost(it) },
                            onReportPost = { reportingPostId = it },
                            onReportUser = { displayUser?.let { reportingUserId = it.id } },
                            onBlockUser = { displayUser?.let { vm.blockUser(it.id) } },
                            onUserClick = { vm.openUserProfile(it) }
                        )
                    }
                    AppNavDestination.SETTINGS -> {
                        SettingsScreen(
                            user = currentUser,
                            profile = currentProfile,
                            isDarkMode = isDarkMode,
                            blockedUsers = blockedUsers,
                            onToggleDarkMode = { vm.toggleDarkMode() },
                            onEditProfile = { vm.setModal(AuthModalState.EDIT_PROFILE) },
                            onSwitchDemoUser = { vm.switchDemoUser(it) },
                            onUnblockUser = { vm.unblockUser(it) },
                            onOpenAdmin = { vm.setNav(AppNavDestination.ADMIN) },
                            onLogout = { vm.logout() },
                            onSignIn = { vm.setModal(AuthModalState.LOGIN) }
                        )
                    }
                    AppNavDestination.ADMIN -> {
                        AdminScreen(
                            metrics = adminMetrics,
                            reports = adminReports,
                            allUsers = suggestedUsers,
                            onToggleBanUser = { vm.toggleBanUser(it) },
                            onUpdateReportStatus = { rId, status -> vm.updateReportStatus(rId, status) },
                            onDeletePost = { vm.deletePost(it) }
                        )
                    }
                }
            }
        }
    }

    // Comments Bottom Sheet
    if (activeCommentPostId != null) {
        CommentSheet(
            comments = activeComments,
            currentUserId = currentUser?.id,
            onClose = { vm.closeComments() },
            onAddComment = { content, parentId ->
                activeCommentPostId?.let { vm.addComment(it, content, parentId) }
            },
            onDeleteComment = { cId ->
                activeCommentPostId?.let { vm.deleteComment(cId, it) }
            },
            onLikeComment = { cId ->
                vm.toggleCommentLike(cId)
            },
            onUserClick = { uId ->
                vm.closeComments()
                vm.openUserProfile(uId)
            }
        )
    }

    // Auth & Content Dialogs
    when (authModalState) {
        AuthModalState.LOGIN -> {
            LoginDialog(
                onDismiss = { vm.setModal(AuthModalState.NONE) },
                onLogin = { id, pass -> vm.login(id, pass) { _, _ -> } },
                onSwitchToSignUp = { vm.setModal(AuthModalState.SIGN_UP) },
                onForgotPassword = { vm.setModal(AuthModalState.FORGOT_PASSWORD) },
                onQuickDemoLogin = { username -> vm.switchDemoUser(username) }
            )
        }
        AuthModalState.SIGN_UP -> {
            SignUpDialog(
                onDismiss = { vm.setModal(AuthModalState.NONE) },
                onSignUp = { u, e, f, p -> vm.signUp(u, e, f, p) { _, _ -> } },
                onSwitchToLogin = { vm.setModal(AuthModalState.LOGIN) }
            )
        }
        AuthModalState.FORGOT_PASSWORD -> {
            ForgotPasswordDialog(
                onDismiss = { vm.setModal(AuthModalState.NONE) },
                onResetPassword = { email, pass -> vm.resetPassword(email, pass) { _, _ -> } }
            )
        }
        AuthModalState.CREATE_POST -> {
            CreatePostDialog(
                currentProfile = currentProfile,
                onDismiss = { vm.setModal(AuthModalState.NONE) },
                onSubmitPost = { content, img, vid ->
                    vm.createPost(content, img, vid)
                }
            )
        }
        AuthModalState.EDIT_PROFILE -> {
            EditProfileDialog(
                profile = currentProfile,
                onDismiss = { vm.setModal(AuthModalState.NONE) },
                onSave = { name, bio, loc, web, av, cov, priv ->
                    vm.updateProfile(name, bio, loc, web, av, cov, priv)
                }
            )
        }
        AuthModalState.REPORT_CONTENT -> {
            val targetId = modalTargetPostId ?: "unknown"
            ReportDialog(
                targetId = targetId,
                targetType = "POST",
                onDismiss = { vm.setModal(AuthModalState.NONE) },
                onSubmitReport = { reason ->
                    vm.reportContent("POST", targetId, reason)
                }
            )
        }
        AuthModalState.NONE -> {}
    }

    // Edit Post Dialog
    if (editingPostItem != null) {
        EditPostDialog(
            initialContent = editingPostItem!!.post.content,
            onDismiss = { editingPostItem = null },
            onSave = { newContent ->
                vm.editPost(editingPostItem!!.post.id, newContent)
                editingPostItem = null
            }
        )
    }

    // Report Post Dialog
    if (reportingPostId != null) {
        ReportDialog(
            targetId = reportingPostId!!,
            targetType = "POST",
            onDismiss = { reportingPostId = null },
            onSubmitReport = { reason ->
                vm.reportContent("POST", reportingPostId!!, reason)
                reportingPostId = null
            }
        )
    }

    // Report User Dialog
    if (reportingUserId != null) {
        ReportDialog(
            targetId = reportingUserId!!,
            targetType = "USER",
            onDismiss = { reportingUserId = null },
            onSubmitReport = { reason ->
                vm.reportContent("USER", reportingUserId!!, reason)
                reportingUserId = null
            }
        )
    }
}
