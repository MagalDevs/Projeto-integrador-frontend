package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.projeto_integrador.session.SessionManager;

public class InicioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio);

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

        // Delay da SplashScreen
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            SessionManager session = new SessionManager(this);

            Intent intent;

            if (session.isLoggedIn()) {
                // Já logado → vai direto para a tela correta
                if (session.isAdmin()) {
                    intent = new Intent(InicioActivity.this, AdminDenunciasActivity.class);
                } else {
                    intent = new Intent(InicioActivity.this, MainActivity.class);
                }
            } else {
                // Não logado → tela de login
                intent = new Intent(InicioActivity.this, LoginActivity.class);
            }

            startActivity(intent);

            // Fecha a SplashScreen
            finish();

        }, 2500); // 2.5 segundos
    }
}