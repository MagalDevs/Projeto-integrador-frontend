package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private TextView textCadastro;
    private TextView textAcessoAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        textCadastro = findViewById(R.id.textCadastro);

        textAcessoAdmin = findViewById(R.id.textAcessoAdmin);

    }

    private void configurarEventos() {

        textCadastro.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);

        });

        // Acesso temporário ao painel admin (sem autenticação)
        textAcessoAdmin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    AdminDenunciasActivity.class
            );

            startActivity(intent);

        });

    }
}