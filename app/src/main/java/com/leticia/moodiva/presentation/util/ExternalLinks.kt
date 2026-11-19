package com.leticia.moodiva.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.leticia.moodiva.domain.model.Track

/**
 * Abre a música no Spotify ou no YouTube por **busca**, sem API (RF17).
 *
 * O texto "artista música" é codificado com `Uri.encode`. Se o app correspondente estiver
 * instalado, o Android pode oferecê-lo; senão, abre no navegador.
 */
object ExternalLinks {

    fun spotifySearch(track: Track): Uri =
        Uri.parse("https://open.spotify.com/search/" + Uri.encode(query(track)))

    fun youtubeSearch(track: Track): Uri =
        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query(track)))

    const val LASTFM_URL = "https://www.last.fm"

    private fun query(track: Track) = "${track.artist} ${track.name}"

    /** Abre o link; devolve `false` se nenhum app puder abri-lo. */
    fun open(context: Context, uri: Uri): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
