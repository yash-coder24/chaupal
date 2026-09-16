package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PostItem
import com.example.data.model.ProfileEntity
import com.example.data.model.UserEntity
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar

enum class ProfileTab { POSTS, SAVED, ABOUT }

@Composable
fun ProfileScreen(
    user: UserEntity?,
    profile: ProfileEntity?,
    currentUserId: String?,
    posts: List<PostItem>,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onStartMessage: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onEditPost: (PostItem) -> Unit,
    onDeletePost: (String) -> Unit,
    onReportPost: (String) -> Unit,
    onReportUser: () -> Unit,
    onBlockUser: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (user == null || profile == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isMe = currentUserId == user.id
    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var menuExpanded by remember { mutableStateOf(false) }

    val myPosts = remember(posts, user.id) {
        posts.filter { it.post.authorId == user.id }
    }
    val savedPosts = remember(posts) {
        posts.filter { it.isSavedByMe }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cover & Avatar Header
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Cover Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    if (profile.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Cover Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Avatar positioned overlapping cover
                UserAvatar(
                    avatarUrl = profile.avatarUrl,
                    displayName = profile.fullName,
                    size = 90.dp,
                    isOnline = user.isOnline,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp)
                        .offset(y = 45.dp)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )

                // Top right actions: Settings / More menu
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    if (isMe) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    } else {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Report Profile") },
                                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onReportUser()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block User", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Block,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onBlockUser()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Profile Bio, Metadata & Action Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp)
            ) {
                // Name & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.fullName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (user.isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "STAFF",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (profile.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata: Location & Website
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (profile.location.isNotBlank()) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = profile.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    if (profile.website.isNotBlank()) {
                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = "Website",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = profile.website.removePrefix("https://").removePrefix("http://"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row: Followers, Following, Posts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(number = "${profile.postsCount}", label = "Posts")
                    StatItem(number = "${profile.followersCount}", label = "Followers")
                    StatItem(number = "${profile.followingCount}", label = "Following")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isMe) {
                        Button(
                            onClick = onEditProfile,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("edit_profile_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settings")
                        }
                    } else {
                        Button(
                            onClick = onToggleFollow,
                            colors = if (isFollowing) ButtonDefaults.outlinedButtonColors()
                            else ButtonDefaults.buttonColors(),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("profile_follow_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onStartMessage,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("profile_message_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message")
                        }
                    }
                }
            }
        }

        // Profile Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == ProfileTab.POSTS,
                    onClick = { selectedTab = ProfileTab.POSTS },
                    text = { Text("Posts (${myPosts.size})") }
                )
                if (isMe) {
                    Tab(
                        selected = selectedTab == ProfileTab.SAVED,
                        onClick = { selectedTab = ProfileTab.SAVED },
                        text = { Text("Saved (${savedPosts.size})") }
                    )
                }
                Tab(
                    selected = selectedTab == ProfileTab.ABOUT,
                    onClick = { selectedTab = ProfileTab.ABOUT },
                    text = { Text("About") }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            ProfileTab.POSTS -> {
                if (myPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No posts yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(myPosts, key = { it.post.id }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PostCard(
                                item = item,
                                currentUserId = currentUserId,
                                onUserClick = onUserClick,
                                onLikeClick = { onLikeClick(item.post.id) },
                                onCommentClick = { onCommentClick(item.post.id) },
                                onShareClick = { onShareClick(item.post.id) },
                                onBookmarkClick = { onBookmarkClick(item.post.id) },
                                onHashtagClick = onHashtagClick,
                                onEditClick = { onEditPost(item) },
                                onDeleteClick = { onDeletePost(item.post.id) },
                                onReportClick = { onReportPost(item.post.id) }
                            )
                        }
                    }
                }
            }
            ProfileTab.SAVED -> {
                if (savedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No bookmarked posts yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(savedPosts, key = { it.post.id }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PostCard(
                                item = item,
                                currentUserId = currentUserId,
                                onUserClick = onUserClick,
                                onLikeClick = { onLikeClick(item.post.id) },
                                onCommentClick = { onCommentClick(item.post.id) },
                                onShareClick = { onShareClick(item.post.id) },
                                onBookmarkClick = { onBookmarkClick(item.post.id) },
                                onHashtagClick = onHashtagClick,
                                onEditClick = { onEditPost(item) },
                                onDeleteClick = { onDeletePost(item.post.id) },
                                onReportClick = { onReportPost(item.post.id) }
                            )
                        }
                    }
                }
            }
            ProfileTab.ABOUT -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("About Community Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Username: @${user.username}")
                            Text("Full Name: ${profile.fullName}")
                            Text("Email: ${user.email}")
                            Text("Location: ${profile.location.ifBlank { "Not specified" }}")
                            Text("Website: ${profile.website.ifBlank { "None" }}")
                            Text("Account Type: ${if (user.isAdmin) "Platform Administrator" else "Community Member"}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
