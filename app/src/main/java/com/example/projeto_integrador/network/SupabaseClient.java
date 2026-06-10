package com.example.projeto_integrador.network;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.projeto_integrador.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Cliente centralizado para comunicação com o Supabase.
 * Fornece um OkHttpClient pré-configurado com autenticação e headers necessários.
 */
public class SupabaseClient {

    private static final String TAG = "SupabaseClient";

    // Constantes obtidas via BuildConfig (configuradas no local.properties).
    // Se não usou local.properties, substitua pelas strings direto aqui.
    public static final String BASE_URL = BuildConfig.SUPABASE_URL;
    public static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private static volatile OkHttpClient httpClient;

    /**
     * Retorna a instância única do OkHttpClient.
     * Inclui um Interceptor que adiciona 'apikey' e 'Authorization' automaticamente.
     */
    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (SupabaseClient.class) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .addInterceptor(new SupabaseInterceptor())
                            .build();
                }
            }
        }
        return httpClient;
    }

    /**
     * Interceptor para injetar headers obrigatórios do Supabase em cada requisição.
     */
    private static class SupabaseInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request original = chain.request();

            // Validação simples para auxiliar o desenvolvedor no início do projeto
            if (ANON_KEY == null || ANON_KEY.isEmpty()) {
                Log.e(TAG, "AVISO: SUPABASE_ANON_KEY não encontrada no local.properties!");
            }

            Request request = original.newBuilder()
                    .header("apikey",        ANON_KEY != null ? ANON_KEY : "")
                    .header("Authorization", "Bearer " + (ANON_KEY != null ? ANON_KEY : ""))
                    .header("Content-Type",  "application/json")
                    .build();

            return chain.proceed(request);
        }
    }

    /**
     * Concatena a BASE_URL com o endpoint de forma segura.
     * @param endpoint O caminho do recurso (ex: "rest/v1/usuarios")
     */
    public static String buildUrl(String endpoint) {
        if (BASE_URL == null || BASE_URL.isEmpty()) {
            Log.e(TAG, "ERRO: SUPABASE_URL não está configurada no local.properties!");
            return endpoint;
        }

        String baseUrl = BASE_URL.endsWith("/") ? BASE_URL : BASE_URL + "/";
        String path = (endpoint != null && endpoint.startsWith("/")) ? endpoint.substring(1) : endpoint;

        return baseUrl + path;
    }

    /**
     * Retorna um Request.Builder já com a URL configurada.
     * Nota: Os headers de autenticação são injetados automaticamente pelo interceptor de 'getHttpClient()'.
     */
    public static Request.Builder requestBuilder(String endpoint) {
        return new Request.Builder()
                .url(buildUrl(endpoint));
    }
}
