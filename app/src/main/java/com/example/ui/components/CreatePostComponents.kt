package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ProfileEntity

val sampleImagePresets = listOf(
    "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&q=80" to "Community Gathering",
    "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&q=80" to "Mountains & Nature",
    "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80" to "Coding Workspace",
    "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80" to "Art & Design",
    "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=800&q=80" to "Travel Roadtrip"
)

@Composable
fun CreatePostCard(
    currentProfile: ProfileEntity?,
    onOpenCreateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenCreateDialog)
            .testTag("create_post_card_trigger"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarUrl = currentProfile?.avatarUrl,
                displayName = currentProfile?.fullName ?: "Me",
                size = 42.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Share with your Chaupal...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = onOpenCreateDialog,
                modifier = Modifier.testTag("create_post_image_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "Add Media",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    currentProfile: ProfileEntity?,
    onDismiss: () -> Unit,
    onSubmitPost: (content: String, imageUrl: String?, videoUrl: String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var customImageUrlInput by remember { mutableStateOf("") }
    var showUrlInput by remember { mutableStateOf(false) }
    var isVideoType by remember { mutableStateOf(false) }

    val quickTags = listOf("#chaupal", "#tech", "#india", "#buildinpublic", "#photography", "#design")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("create_post_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Create Post",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        avatarUrl = currentProfile?.avatarUrl,
                        displayName = currentProfile?.fullName ?: "Me",
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentProfile?.fullName ?: "Anonymous",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Posting to Public Feed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Post Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("What's happening in your community? Share an update, thought, or story...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 180.dp)
                        .testTag("create_post_text_field"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Hashtag Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTags.forEach { tag ->
                        FilterChip(
                            selected = content.contains(tag),
                            onClick = {
                                if (!content.contains(tag)) {
                                    content = if (content.isBlank()) tag else "$content $tag"
                                }
                            },
                            label = { Text(tag, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Preview if selected
                if (!selectedImageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selected image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { selectedImageUrl = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove media", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Preset photos row
                Text(
                    text = "Add Photo or Visual:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleImagePresets.forEach { (url, label) ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedImageUrl = url }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (content.isNotBlank() || !selectedImageUrl.isNullOrBlank()) {
                            onSubmitPost(
                                content.trim(),
                                selectedImageUrl,
                                if (isVideoType) "sample_video_stream" else null
                            )
                        }
                    },
                    enabled = content.isNotBlank() || !selectedImageUrl.isNullOrBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("publish_post_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish to Chaupal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
