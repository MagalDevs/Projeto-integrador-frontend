package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import com.example.projeto_integrador.session.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editSenha;
    private MaterialButton buttonLogin;
    private TextView textCadastro;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Se já está logado, redireciona direto
        if (session.isLoggedIn()) {
            redirecionarPorRole();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

        iniciarComponentes();
        configurarEventos();
    }

    private void iniciarComponentes() {
        editEmail    = findViewById(R.id.editEmail);
        editSenha    = findViewById(R.id.editSenha);
        buttonLogin  = findViewById(R.id.buttonLogin);
        textCadastro = findViewById(R.id.textCadastro);
    }

    private void configurarEventos() {

        // Botão "Entrar"
        buttonLogin.setOnClickListener(v -> {
            // TODO: Integrar com endpoint de login quando estiver pronto no backend
            Snackbar.make(
                    findViewById(R.id.main),
                    "⚠️ Login será integrado em breve. Use o cadastro para criar uma conta.",
                    Snackbar.LENGTH_LONG
            ).show();
        });

        // Link "Cadastre-se"
        textCadastro.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }

    /**
     * Redireciona o usuário para a tela correta baseado na role.
     * ADMIN → AdminDenunciasActivity
     * USER  → MainActivity (mapa)
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
}