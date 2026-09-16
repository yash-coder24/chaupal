package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostItem
import com.example.data.model.SearchResult
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar

enum class SearchTab { ALL, PEOPLE, POSTS, TAGS }

@Composable
fun SearchScreen(
    query: String,
    searchResult: SearchResult,
    onQueryChange: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
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
    var selectedTab by remember { mutableStateOf(SearchTab.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Search Input Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search Chaupal...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_query_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchTab.values().forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Search Results List
        if (query.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type keywords, names, or #hashtags to explore Chaupal",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // People Section
                if (selectedTab == SearchTab.ALL || selectedTab == SearchTab.PEOPLE) {
                    if (searchResult.users.isNotEmpty()) {
                        item {
                            Text(
                                text = "People (${searchResult.users.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(searchResult.users, key = { it.user.id }) { u ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserClick(u.user.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        avatarUrl = u.profile.avatarUrl,
                                        displayName = u.profile.fullName,
                                        size = 46.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = u.profile.fullName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "@${u.user.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (u.profile.bio.isNotBlank()) {
                                            Text(
                                                text = u.profile.bio,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    if (currentUserId != u.user.id) {
                                        Button(
                                            onClick = { onToggleFollow(u.user.id) },
                                            shape = RoundedCornerShape(18.dp),
                                            colors = if (u.isFollowedByMe) ButtonDefaults.outlinedButtonColors()
                                            else ButtonDefaults.buttonColors(),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = if (u.isFollowedByMe) "Following" else "Follow",
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Hashtags Section
                if (selectedTab == SearchTab.ALL || selectedTab == SearchTab.TAGS) {
                    if (searchResult.hashtags.isNotEmpty()) {
                        item {
                            Text(
                                text = "Hashtags (${searchResult.hashtags.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(searchResult.hashtags, key = { it.tag }) { tag ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTagClick(tag.tag) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tag.tag,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tag.postCount} posts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Posts Section
                if (selectedTab == SearchTab.ALL || selectedTab == SearchTab.POSTS) {
                    if (searchResult.posts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Posts (${searchResult.posts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(searchResult.posts, key = { it.post.id }) { postItem ->
                            PostCard(
                                item = postItem,
                                currentUserId = currentUserId,
                                onUserClick = onUserClick,
                                onLikeClick = { onLikeClick(postItem.post.id) },
                                onCommentClick = { onCommentClick(postItem.post.id) },
                                onShareClick = { onShareClick(postItem.post.id) },
                                onBookmarkClick = { onBookmarkClick(postItem.post.id) },
                                onHashtagClick = onTagClick,
                                onEditClick = { onEditClick(postItem) },
                                onDeleteClick = { onDeleteClick(postItem.post.id) },
                                onReportClick = { onReportClick(postItem.post.id) }
                            )
                        }
                    }
                }

                if (searchResult.users.isEmpty() && searchResult.hashtags.isEmpty() && searchResult.posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No results matching \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
