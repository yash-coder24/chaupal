package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostItem
import com.example.data.model.ProfileEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserSummary
import com.example.ui.FeedFilter
import com.example.ui.components.CreatePostCard
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar

@Composable
fun FeedScreen(
    posts: List<PostItem>,
    currentFilter: FeedFilter,
    currentUser: UserEntity?,
    currentProfile: ProfileEntity?,
    suggestedUsers: List<UserSummary>,
    onFilterChange: (FeedFilter) -> Unit,
    onOpenCreatePost: () -> Unit,
    onUserClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onEditClick: (PostItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("feed_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feed Filter Tabs: For You, Following, Latest
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeedFilterTab(
                        title = "For You",
                        selected = currentFilter == FeedFilter.FOR_YOU,
                        onClick = { onFilterChange(FeedFilter.FOR_YOU) },
                        modifier = Modifier.weight(1f)
                    )
                    FeedFilterTab(
                        title = "Following",
                        selected = currentFilter == FeedFilter.FOLLOWING,
                        onClick = { onFilterChange(FeedFilter.FOLLOWING) },
                        modifier = Modifier.weight(1f)
                    )
                    FeedFilterTab(
                        title = "Latest",
                        selected = currentFilter == FeedFilter.LATEST,
                        onClick = { onFilterChange(FeedFilter.LATEST) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Stories / Community Spotlight Row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Chaupal Community Spotlight",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(modifier = Modifier.width(4.dp))

                    // My story / add post
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(onClick = onOpenCreatePost)
                            .padding(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            UserAvatar(
                                avatarUrl = currentProfile?.avatarUrl,
                                displayName = currentProfile?.fullName ?: "Me",
                                size = 56.dp
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "New story",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your Story",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Suggested users in spotlight
                    suggestedUsers.forEach { userSummary ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onUserClick(userSummary.user.id) }
                                .padding(4.dp)
                        ) {
                            UserAvatar(
                                avatarUrl = userSummary.profile.avatarUrl,
                                displayName = userSummary.profile.fullName,
                                size = 56.dp,
                                isOnline = userSummary.user.isOnline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = userSummary.profile.fullName.split(" ").firstOrNull() ?: userSummary.user.username,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        // Create Post Card Trigger
        item {
            CreatePostCard(
                currentProfile = currentProfile,
                onOpenCreateDialog = onOpenCreatePost
            )
        }

        // Feed Posts
        if (posts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No posts found in this feed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Be the first to share an update or follow more creators!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onOpenCreatePost) {
                            Text("Create First Post")
                        }
                    }
                }
            }
        } else {
            items(posts, key = { it.post.id }) { item ->
                PostCard(
                    item = item,
                    currentUserId = currentUser?.id,
                    onUserClick = onUserClick,
                    onLikeClick = { onLikeClick(item.post.id) },
                    onCommentClick = { onCommentClick(item.post.id) },
                    onShareClick = { onShareClick(item.post.id) },
                    onBookmarkClick = { onBookmarkClick(item.post.id) },
                    onHashtagClick = onHashtagClick,
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item.post.id) },
                    onReportClick = { onReportClick(item.post.id) }
                )
            }
        }
    }
}

@Composable
private fun FeedFilterTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
