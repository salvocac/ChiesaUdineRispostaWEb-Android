package com.caccavo.chiesaudinerispostaweb.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caccavo.chiesaudinerispostaweb.R

@Composable
fun HomeScreen(
    onOpenDailyVerse: () -> Unit,
    onOpenBible: () -> Unit,
    onOpenAudioSettings: () -> Unit,
    onOpenUserGuide: () -> Unit
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFF1565C0).copy(alpha = 0.08f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(top = 10.dp)
            )

            Spacer(Modifier.height(8.dp))

            HomeActionButton(
                icon = Icons.Filled.FormatQuote,
                text = "Versetto del giorno",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                onClick = onOpenDailyVerse
            )

            HomeActionButton(
                icon = Icons.Filled.MenuBook,
                text = "Apri Bibbia",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                onClick = onOpenBible
            )

            HomeActionButton(
                icon = Icons.Filled.Language,
                text = "Sito Web",
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White,
                onClick = { openUrl("https://www.chiesacristianaudine.it") }
            )

            HomeActionButton(
                icon = Icons.Filled.Map,
                text = "Dove siamo",
                containerColor = Color(0xFF00796B),
                contentColor = Color.White,
                onClick = { openUrl("https://maps.google.com/?q=Via+Croazia+14+33100+Udine") }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(35.dp),
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                SocialIconButton(R.drawable.youtube) {
                    openUrl("https://www.youtube.com/channel/UCqtwkH2xz1fFoTObbDXcpOw")
                }
                SocialIconButton(R.drawable.instagram) {
                    openUrl("https://www.instagram.com/chiesaevangelicadiudine/")
                }
                SocialIconButton(R.drawable.facebook) {
                    openUrl("https://www.facebook.com/share/1HxDBZvDhC/?mibextid=wwXIfr")
                }
            }

            Spacer(Modifier.weight(1f))

            HomeActionButton(
                icon = Icons.Filled.VolumeUp,
                text = "Audio Bibbia",
                containerColor = Color(0xFFEF6C00),
                contentColor = Color.White,
                onClick = onOpenAudioSettings
            )

            TextButton(onClick = onOpenUserGuide) {
                Icon(Icons.Filled.Help, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Guida all'uso dell'app", fontWeight = FontWeight.SemiBold)
            }

            Text(
                "© CCE Friulana di Udine",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SocialIconButton(drawableRes: Int, onClick: () -> Unit) {
    Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick)
    )
}
