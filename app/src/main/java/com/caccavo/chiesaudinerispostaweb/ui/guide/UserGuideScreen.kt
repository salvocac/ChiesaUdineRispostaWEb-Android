package com.caccavo.chiesaudinerispostaweb.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class GuideSection(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val body: List<String>
)

private val guideSections = listOf(
    GuideSection(
        icon = Icons.Filled.Home,
        color = Color(0xFF1E88E5),
        title = "Home",
        body = listOf(
            "Dalla schermata iniziale puoi aprire il Versetto del giorno, la Bibbia, il sito web della chiesa e la mappa per raggiungerci.",
            "In basso trovi anche \"Audio Bibbia\", che conferma che tutta la lettura usa gli audio registrati inclusi nell'app."
        )
    ),
    GuideSection(
        icon = Icons.Filled.FormatQuote,
        color = Color(0xFF3949AB),
        title = "Versetto del giorno",
        body = listOf(
            "Ogni giorno l'app propone un versetto diverso, con un breve commento.",
            "Da qui puoi ascoltarlo, condividerlo come immagine o creare un video da inviare."
        )
    ),
    GuideSection(
        icon = Icons.Filled.Search,
        color = Color(0xFF8E24AA),
        title = "Cercare nella Bibbia",
        body = listOf(
            "Nel campo di ricerca puoi scrivere un riferimento (es. \"Giovanni 3:16\") oppure semplicemente una parola.",
            "Se cerchi una parola, l'app mostra tutti i versetti in cui compare in tutta la Bibbia, con una statistica in alto che indica quante volte quella parola è presente."
        )
    ),
    GuideSection(
        icon = Icons.Filled.MenuBook,
        color = Color(0xFF00897B),
        title = "Scegliere libro, capitolo e versetti",
        body = listOf(
            "Sotto il campo di ricerca puoi scegliere il libro, il capitolo e l'intervallo di versetti (\"Da\" / \"A\") che vuoi leggere.",
            "Premi \"Cerca\" per aprire il brano scelto in lettura."
        )
    ),
    GuideSection(
        icon = Icons.Filled.PlayCircle,
        color = Color(0xFF43A047),
        title = "Ascoltare la Bibbia",
        body = listOf(
            "Il pulsante \"Ascolta\" legge ad alta voce il brano selezionato; \"Pausa\" e \"Stop\" controllano la lettura in corso.",
            "Anche dentro la schermata di lettura trovi un pulsante per ascoltare senza dover tornare indietro.",
            "Se interrompi la lettura, la volta dopo trovi il pulsante \"Riprendi da...\" per continuare esattamente da dove avevi lasciato."
        )
    ),
    GuideSection(
        icon = Icons.Filled.Checklist,
        color = Color(0xFFFB8C00),
        title = "Selezionare e condividere i versetti",
        body = listOf(
            "Nella schermata di lettura, tocca \"Seleziona versetti\" e scegli quelli che ti interessano.",
            "Poi puoi condividerli come immagine, oppure creare un video con testo e audio da inviare su WhatsApp o altrove."
        )
    ),
    GuideSection(
        icon = Icons.Filled.VolumeUp,
        color = Color(0xFFD81B60),
        title = "Audio Bibbia",
        body = listOf(
            "Dalla schermata iniziale, \"Audio Bibbia\" ti mostra che l'intera Bibbia è disponibile in audio MP3 e che la lettura usa esclusivamente quegli audio registrati, senza voce sintetizzata."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(onClose: () -> Unit) {
    var searchText by remember { mutableStateOf("") }

    val filteredSections = if (searchText.isBlank()) {
        guideSections
    } else {
        guideSections.filter { section ->
            section.title.contains(searchText, ignoreCase = true) ||
                section.body.any { it.contains(searchText, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guida Utente") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Chiudi")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Cerca nella guida") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (filteredSections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun argomento trovato.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredSections) { section ->
                        GuideCard(section)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCard(section: GuideSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(section.color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(section.icon, contentDescription = null, tint = section.color)
                }
                Spacer(Modifier.width(12.dp))
                Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            section.body.forEach { paragraph ->
                Text(
                    paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}
