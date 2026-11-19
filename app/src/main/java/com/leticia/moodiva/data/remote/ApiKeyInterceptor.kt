package com.leticia.moodiva.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Acrescenta `api_key` e `format=json` a toda requisição ao Last.fm (planejamento, seção 3.2).
 *
 * A chave vem de `BuildConfig.LASTFM_API_KEY` (gerado a partir do `local.properties`) e é
 * recebida pelo construtor: esta classe não conhece o BuildConfig e a chave nunca é
 * escrita no código. O Last.fm só aceita a chave na URL; por isso o log HTTP do app
 * fica desligado no build de release e, em debug, registra só a linha da requisição.
 */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .setQueryParameter(PARAM_API_KEY, apiKey)
            .setQueryParameter(PARAM_FORMAT, "json")
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }

    companion object {
        const val PARAM_API_KEY = "api_key"
        const val PARAM_FORMAT = "format"
    }
}
