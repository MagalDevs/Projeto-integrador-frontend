package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────
    private TextInputLayout  layoutNome, layoutEmail, layoutSenha;
    private TextInputEditText editNome, editEmail, editSenha;
    private MaterialButton    buttonCadastrar;
    private TextView          textJaTemConta;

    // ───────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bindViews();
        setupListeners();
    }

    // ── Bind ────────────────────────────────────────────────
    private void bindViews() {
        editNome    = findViewById(R.id.editNome);
        editEmail   = findViewById(R.id.editEmail);
        editSenha   = findViewById(R.id.editSenha);

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

        // TODO: substituir pela sua lógica de cadastro (Firebase, API, etc.)
        // Exemplo de fluxo de sucesso:
        Snackbar.make(
                findViewById(R.id.main),
                "Conta criada com sucesso!",
                Snackbar.LENGTH_SHORT
        ).show();

        // Após cadastro bem-sucedido → vai para LoginActivity
        navegarParaLogin();
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