// com/example/projeto_integrador/LoginActivity.java
package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.projeto_integrador.network.SupabaseClient;
import com.example.projeto_integrador.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText editEmail, editSenha;
    private MaterialButton    buttonLogin;
    private TextView          textCadastro;
    private View              rootView;

    private SessionManager session;

    // ───────────────────────────── Lifecycle ─────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        if (session.isLoggedIn()) {
            redirecionarPorRole();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        rootView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        iniciarComponentes();
        configurarEventos();
    }

    // ───────────────────────────── Setup ─────────────────────────────────

    private void iniciarComponentes() {
        editEmail    = findViewById(R.id.editEmail);
        editSenha    = findViewById(R.id.editSenha);
        buttonLogin  = findViewById(R.id.buttonLogin);
        textCadastro = findViewById(R.id.textCadastro);
    }

    private void configurarEventos() {

        buttonLogin.setOnClickListener(v -> tentarLogin());

        textCadastro.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    // ───────────────────────────── Login ─────────────────────────────────

    private void tentarLogin() {
        String email = getText(editEmail);
        String senha = getText(editSenha);

        // Validação básica
        if (email.isEmpty()) {
            showSnack("Informe seu e-mail.");
            return;
        }
        if (senha.isEmpty()) {
            showSnack("Informe sua senha.");
            return;
        }

        setCarregando(true);

        // Corpo da requisição
        JsonObject body = new JsonObject();
        body.addProperty("email",    email);
        body.addProperty("password", senha);

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        Request request = SupabaseClient
                .requestBuilder("/auth/v1/token?grant_type=password")
                .post(requestBody)
                .build();

        SupabaseClient.getHttpClient().newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Erro de rede", e);
                runOnUiThread(() -> {
                    setCarregando(false);
                    showSnack("Sem conexão. Verifique a internet.");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Status: " + response.code() + " | Body: " + json);

                runOnUiThread(() -> {
                    setCarregando(false);

                    if (response.isSuccessful()) {
                        processarRespostaLogin(json);
                    } else {
                        String mensagem = extrairMensagemErro(json);
                        showSnack("Erro: " + mensagem);
                    }
                });
            }
        });
    }

    /**
     * Supabase retorna:
     * {
     *   "access_token": "...",
     *   "user": {
     *     "id": "...",
     *     "email": "...",
     *     "app_metadata": { "role": "admin" },   ← definida pelo service_role
     *     "user_metadata": { "role": "user" }    ← definida no cadastro
     *   }
     * }
     */
    private void processarRespostaLogin(String json) {
        try {
            JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
            JsonObject user   = root.getAsJsonObject("user");
            String     userId = user.get("id").getAsString();
            String     email  = user.get("email").getAsString();

            // Log completo para debug
            Log.d(TAG, "user_metadata: " + user.get("user_metadata"));
            Log.d(TAG, "app_metadata: "  + user.get("app_metadata"));

            String nome = email;
            if (user.has("user_metadata")) {
                JsonObject meta = user.getAsJsonObject("user_metadata");
                if (meta.has("name") && !meta.get("name").isJsonNull()) {
                    nome = meta.get("name").getAsString();
                }
            }

            String role = "USER";
            if (user.has("app_metadata")) {
                JsonObject appMeta = user.getAsJsonObject("app_metadata");
                if (appMeta.has("role") && !appMeta.get("role").isJsonNull()) {
                    role = appMeta.get("role").getAsString().toUpperCase();
                }
            }
            if ("USER".equals(role) && user.has("user_metadata")) {
                JsonObject userMeta = user.getAsJsonObject("user_metadata");
                if (userMeta.has("role") && !userMeta.get("role").isJsonNull()) {
                    role = userMeta.get("role").getAsString().toUpperCase();
                }
            }

            Log.d(TAG, "Role final salva: " + role); // ← veja aqui o que está sendo salvo

            session.saveUser(userId, nome, email, role);
            redirecionarPorRole();

        } catch (Exception e) {
            Log.e(TAG, "Erro ao parsear resposta", e);
            showSnack("Resposta inesperada do servidor.");
        }
    }

    private String extrairMensagemErro(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("error_description")) return obj.get("error_description").getAsString();
            if (obj.has("message"))           return obj.get("message").getAsString();
            if (obj.has("msg"))               return obj.get("msg").getAsString();
        } catch (Exception ignored) {}
        return "Credenciais inválidas.";
    }

    // ───────────────────────────── Navegação ─────────────────────────────

    private void redirecionarPorRole() {
        Intent intent = session.isAdmin()
                ? new Intent(this, AdminDenunciasActivity.class)
                : new Intent(this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ───────────────────────────── Helpers ───────────────────────────────

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void setCarregando(boolean carregando) {
        buttonLogin.setEnabled(!carregando);
        buttonLogin.setText(carregando ? "Entrando…" : "Entrar");
    }

    private void showSnack(String msg) {
        Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG).show();
    }
}