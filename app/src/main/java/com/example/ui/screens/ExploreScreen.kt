package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.HashtagEntity
import com.example.data.model.PostItem
import com.example.data.model.UserSummary
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar

@Composable
fun ExploreScreen(
    trendingHashtags: List<HashtagEntity>,
    suggestedUsers: List<UserSummary>,
    allPosts: List<PostItem>,
    selectedHashtag: String?,
    hashtagPosts: List<PostItem>,
    onSelectHashtag: (String) -> Unit,
    onClearHashtag: () -> Unit,
    onSearchClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onEditClick: (PostItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar trigger
        item {
            Surface(
                onClick = onSearchClick,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("explore_search_bar_trigger"),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search users, posts, #hashtags on Chaupal...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Hashtag Filter Active Banner
        if (selectedHashtag != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = selectedHashtag,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${hashtagPosts.size} posts found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = onClearHashtag) {
                            Icon(Icons.Default.Close, contentDescription = "Clear tag", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Render Hashtag Posts
            items(hashtagPosts, key = { it.post.id }) { item ->
                PostCard(
                    item = item,
                    currentUserId = currentUserId,
                    onUserClick = onUserClick,
                    onLikeClick = { onLikeClick(item.post.id) },
                    onCommentClick = { onCommentClick(item.post.id) },
                    onShareClick = { onShareClick(item.post.id) },
                    onBookmarkClick = { onBookmarkClick(item.post.id) },
                    onHashtagClick = onSelectHashtag,
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item.post.id) },
                    onReportClick = { onReportClick(item.post.id) }
                )
            }
        } else {
            // Trending Hashtags Row
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trending on Chaupal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trendingHashtags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { onSelectHashtag(tag.tag) },
                                    label = {
                                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text(tag.tag, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${tag.postCount} posts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recommended Accounts Carousel
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Creators You Might Like",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            suggestedUsers.forEach { userSummary ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.width(140.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        UserAvatar(
                                            avatarUrl = userSummary.profile.avatarUrl,
                                            displayName = userSummary.profile.fullName,
                                            size = 52.dp,
                                            onClick = { onUserClick(userSummary.user.id) }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = userSummary.profile.fullName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "@${userSummary.user.username}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { onToggleFollow(userSummary.user.id) },
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            colors = if (userSummary.isFollowedByMe)
                                                ButtonDefaults.outlinedButtonColors()
                                            else ButtonDefaults.buttonColors(),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(
                                                text = if (userSummary.isFollowedByMe) "Following" else "Follow",
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Media Grid (Visual posts showcase)
            item {
                Text(
                    text = "Explore Media",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Grid of photo posts
            val mediaPosts = allPosts.filter { !it.post.imageUrl.isNullOrBlank() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunks = mediaPosts.chunked(3)
                    chunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { postItem ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onCommentClick(postItem.post.id) }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(postItem.post.imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Explore media",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (postItem.post.mediaType == "VIDEO") {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Video",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(20.dp)
                                        )
                                    }
                                }
                            }
                            // Fill remaining space if row has less than 3
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
