package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.projeto_integrador.network.ApiClient;
import com.example.projeto_integrador.network.ApiConfig;
import com.example.projeto_integrador.session.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────
    private TextInputLayout   layoutNome, layoutEmail, layoutSenha;
    private TextInputEditText editNome, editEmail, editSenha;
    private MaterialButton    buttonCadastrar;
    private TextView          textJaTemConta;

    private SessionManager session;

    // ───────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        session = new SessionManager(this);

        bindViews();
        setupListeners();
    }

    // ── Bind ────────────────────────────────────────────────
    private void bindViews() {
        editNome    = findViewById(R.id.editNome);
        editEmail   = findViewById(R.id.editEmail);
        editSenha   = findViewById(R.id.editSenha);

        // Obtém os TextInputLayout pais dos EditTexts
        layoutNome  = (TextInputLayout) editNome.getParent().getParent();
        layoutEmail = (TextInputLayout) editEmail.getParent().getParent();
        layoutSenha = (TextInputLayout) editSenha.getParent().getParent();

        buttonCadastrar = findViewById(R.id.buttonCadastrar);
        textJaTemConta  = findViewById(R.id.textJaTemConta);
    }

    // ── Listeners ───────────────────────────────────────────
    private void setupListeners() {

        // Botão "Criar conta"
        buttonCadastrar.setOnClickListener(v -> {
            if (validarCampos()) {
                realizarCadastro();
            }
        });

        // ── Ancoragem para LoginActivity ──────────────────
        // Toca em "Já possui conta? Faça login" → volta para Login
        textJaTemConta.setOnClickListener(v -> navegarParaLogin());
    }

    // ── Validação ───────────────────────────────────────────
    private boolean validarCampos() {
        boolean valido = true;

        // Limpa erros anteriores
        layoutNome.setError(null);
        layoutEmail.setError(null);
        layoutSenha.setError(null);

        String nome  = getText(editNome);
        String email = getText(editEmail);
        String senha = getText(editSenha);

        if (TextUtils.isEmpty(nome)) {
            layoutNome.setError("Informe seu nome completo");
            valido = false;
        }

        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Informe seu e-mail");
            valido = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("E-mail inválido");
            valido = false;
        }

        if (TextUtils.isEmpty(senha)) {
            layoutSenha.setError("Informe uma senha");
            valido = false;
        } else if (senha.length() < 6) {
            layoutSenha.setError("A senha deve ter ao menos 6 caracteres");
            valido = false;
        }

        return valido;
    }

    // ── Cadastro ────────────────────────────────────────────
    private void realizarCadastro() {
        String nome  = getText(editNome);
        String email = getText(editEmail);
        String senha = getText(editSenha);

        // Feedback visual: desabilita botão durante a requisição
        buttonCadastrar.setEnabled(false);
        buttonCadastrar.setText("Criando conta...");

        try {
            JSONObject body = new JSONObject();
            body.put("nome", nome);
            body.put("email", email);
            body.put("password", senha);

            ApiClient.getInstance().post(
                    ApiConfig.USUARIOS,
                    body,
                    // ── Sucesso ──────────────────────────────
                    response -> {
                        try {
                            JSONObject user = new JSONObject(response);

                            String id   = user.getString("id");
                            String name = user.getString("nome");
                            String mail = user.getString("email");
                            String role = user.getString("role");

                            // Salva sessão
                            session.saveUser(id, name, mail, role);

                            Snackbar.make(
                                    findViewById(R.id.main),
                                    "✅ Conta criada com sucesso!",
                                    Snackbar.LENGTH_SHORT
                            ).show();

                            // Redireciona com base na role
                            redirecionarPorRole();

                        } catch (JSONException e) {
                            restaurarBotao();
                            Snackbar.make(
                                    findViewById(R.id.main),
                                    "Erro ao processar resposta do servidor.",
                                    Snackbar.LENGTH_LONG
                            ).show();
                        }
                    },
                    // ── Erro ─────────────────────────────────
                    (statusCode, message) -> {
                        restaurarBotao();

                        String errorMsg;
                        if (statusCode == 409) {
                            errorMsg = "Este e-mail já está cadastrado.";
                        } else if (statusCode == 400) {
                            errorMsg = "Dados inválidos. Verifique os campos.";
                        } else if (statusCode == -1) {
                            errorMsg = "Sem conexão com o servidor. Verifique sua rede.";
                        } else {
                            errorMsg = "Erro no servidor (código " + statusCode + ").";
                        }

                        Snackbar.make(
                                findViewById(R.id.main),
                                "❌ " + errorMsg,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
            );

        } catch (JSONException e) {
            restaurarBotao();
            Snackbar.make(
                    findViewById(R.id.main),
                    "Erro ao montar requisição.",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private void restaurarBotao() {
        buttonCadastrar.setEnabled(true);
        buttonCadastrar.setText("Criar conta");
    }

    /**
     * Redireciona com base na role do usuário.
     * ADMIN → AdminDenunciasActivity
     * USER  → MainActivity
     */
    private void redirecionarPorRole() {
        Intent intent;

        if (session.isAdmin()) {
            intent = new Intent(this, AdminDenunciasActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Navegação ───────────────────────────────────────────

    /**
     * Ancora de volta ao LoginActivity.
     * FLAG_ACTIVITY_CLEAR_TOP garante que não empilhe uma nova instância
     * caso o Login já esteja na back-stack.
     */
    private void navegarParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // remove RegisterActivity da pilha
    }

    // ── Util ────────────────────────────────────────────────
    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}