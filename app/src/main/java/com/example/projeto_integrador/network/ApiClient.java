package com.example.projeto_integrador.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente HTTP para comunicação com o backend.
 * <p>
 * Usa {@link HttpURLConnection} (nativo do Android) — sem dependências externas.
 * Todas as chamadas de rede rodam em background via {@link ExecutorService};
 * os callbacks são despachados na <b>main thread</b>.
 *
 * <h3>Uso típico:</h3>
 * <pre>
 * ApiClient.getInstance().post("/usuarios", body,
 *     response -> { /* sucesso — main thread *&#47; },
 *     error    -> { /* erro    — main thread *&#47; }
 * );
 * </pre>
 */
public final class ApiClient {

    private static final String TAG = "ApiClient";
    private static final int TIMEOUT_MS = 15_000;

    private static ApiClient instance;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Singleton ────────────────────────────────────────────────────────────

    private ApiClient() { }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    // ── Callbacks ────────────────────────────────────────────────────────────

    public interface OnSuccessListener {
        void onSuccess(String responseBody);
    }

    public interface OnErrorListener {
        void onError(int statusCode, String message);
    }

    // ── GET ──────────────────────────────────────────────────────────────────

    /**
     * Executa um GET assíncrono.
     */
    public void get(String path,
                    OnSuccessListener onSuccess,
                    OnErrorListener onError) {

        executor.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(path, "GET");
                handleResponse(conn, onSuccess, onError);
            } catch (IOException e) {
                postError(onError, -1, "Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── POST (JSON) ──────────────────────────────────────────────────────────

    /**
     * Executa um POST assíncrono com body JSON.
     */
    public void post(String path,
                     JSONObject body,
                     OnSuccessListener onSuccess,
                     OnErrorListener onError) {

        executor.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(path, "POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                writeBody(conn, body.toString());
                handleResponse(conn, onSuccess, onError);
            } catch (IOException e) {
                postError(onError, -1, "Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── POST (Multipart) ─────────────────────────────────────────────────────

    /**
     * Executa um POST multipart/form-data assíncrono.
     *
     * @param textFields  campos de texto (chave → valor)
     * @param fileFields  campos de arquivo (chave → array de bytes). Pode ser null.
     * @param fileNames   nomes dos arquivos (chave → nome). Pode ser null.
     */
    public void postMultipart(String path,
                              Map<String, String> textFields,
                              Map<String, byte[]> fileFields,
                              Map<String, String> fileNames,
                              OnSuccessListener onSuccess,
                              OnErrorListener onError) {

        executor.execute(() -> {
            String boundary = "----Boundary" + UUID.randomUUID().toString();

            try {
                HttpURLConnection conn = openConnection(path, "POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setDoOutput(true);

                try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {

                    // Campos de texto
                    if (textFields != null) {
                        for (Map.Entry<String, String> entry : textFields.entrySet()) {
                            out.writeBytes("--" + boundary + "\r\n");
                            out.writeBytes("Content-Disposition: form-data; name=\""
                                    + entry.getKey() + "\"\r\n\r\n");
                            out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                            out.writeBytes("\r\n");
                        }
                    }

                    // Campos de arquivo
                    if (fileFields != null) {
                        for (Map.Entry<String, byte[]> entry : fileFields.entrySet()) {
                            String key = entry.getKey();
                            String fileName = (fileNames != null && fileNames.containsKey(key))
                                    ? fileNames.get(key)
                                    : "file.jpg";

                            out.writeBytes("--" + boundary + "\r\n");
                            out.writeBytes("Content-Disposition: form-data; name=\""
                                    + key + "\"; filename=\"" + fileName + "\"\r\n");
                            out.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                            out.write(entry.getValue());
                            out.writeBytes("\r\n");
                        }
                    }

                    out.writeBytes("--" + boundary + "--\r\n");
                    out.flush();
                }

                handleResponse(conn, onSuccess, onError);
            } catch (IOException e) {
                postError(onError, -1, "Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── PATCH (JSON) ─────────────────────────────────────────────────────────

    /**
     * Executa um PATCH assíncrono com body JSON.
     */
    public void patch(String path,
                      JSONObject body,
                      OnSuccessListener onSuccess,
                      OnErrorListener onError) {

        executor.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(path, "PATCH");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                writeBody(conn, body.toString());
                handleResponse(conn, onSuccess, onError);
            } catch (IOException e) {
                postError(onError, -1, "Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── PATCH (sem body) ─────────────────────────────────────────────────────

    /**
     * Executa um PATCH assíncrono sem body (útil para update-status via path).
     */
    public void patchNoBody(String path,
                            OnSuccessListener onSuccess,
                            OnErrorListener onError) {

        executor.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(path, "PATCH");
                handleResponse(conn, onSuccess, onError);
            } catch (IOException e) {
                postError(onError, -1, "Erro de conexão: " + e.getMessage());
            }
        });
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private HttpURLConnection openConnection(String path, String method)
            throws IOException {

        URL url = new URL(ApiConfig.url(path));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method.equals("PATCH") ? "POST" : method);

        // PATCH não é suportado nativamente por HttpURLConnection em todas as versões
        if (method.equals("PATCH")) {
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        }

        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private void writeBody(HttpURLConnection conn, String body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private void handleResponse(HttpURLConnection conn,
                                OnSuccessListener onSuccess,
                                OnErrorListener onError) throws IOException {

        int code = conn.getResponseCode();

        if (code >= 200 && code < 300) {
            String body = readStream(conn.getInputStream());
            postSuccess(onSuccess, body);
        } else {
            String errorBody = "";
            if (conn.getErrorStream() != null) {
                errorBody = readStream(conn.getErrorStream());
            }
            Log.e(TAG, "HTTP " + code + ": " + errorBody);
            postError(onError, code, errorBody);
        }

        conn.disconnect();
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void postSuccess(OnSuccessListener listener, String body) {
        if (listener != null) {
            mainHandler.post(() -> listener.onSuccess(body));
        }
    }

    private void postError(OnErrorListener listener, int code, String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onError(code, message));
        }
    }
}
