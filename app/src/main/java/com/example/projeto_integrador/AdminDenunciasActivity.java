package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AdminDenunciasActivity extends AppCompatActivity {

    private ImageButton buttonBack;

    private CardView cardDenuncia1;
    private CardView cardDenuncia2;
    private CardView cardDenuncia3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_denuncias);

        iniciarComponentes();

        configurarEventos();
    }

    private void iniciarComponentes() {

        buttonBack = findViewById(R.id.buttonBack);

        cardDenuncia1 = findViewById(R.id.cardDenuncia1);
        cardDenuncia2 = findViewById(R.id.cardDenuncia2);
        cardDenuncia3 = findViewById(R.id.cardDenuncia3);
    }

    private void configurarEventos() {

        // BOTÃO VOLTAR
        buttonBack.setOnClickListener(v -> finish());

        // CARD 1 — EM ANÁLISE
        cardDenuncia1.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AdminDenunciasActivity.this,
                    AdminDetalheDenunciaActivity.class
            );

            // Passa dados mockados via Intent extras
            intent.putExtra("tipo", "🕳️ Buraco na via");
            intent.putExtra("cidadao", "João Silva");
            intent.putExtra("data", "28/05/2026");
            intent.putExtra("descricao", "Existe um buraco muito grande na avenida causando risco aos motoristas. O buraco tem aproximadamente 50cm de diâmetro e está localizado na faixa da direita, próximo ao meio-fio.");
            intent.putExtra("status", "EM ANÁLISE");
            intent.putExtra("latitude", -23.0856931);
            intent.putExtra("longitude", -47.2022082);

            startActivity(intent);
        });

        // CARD 2 — EM ANDAMENTO
        cardDenuncia2.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AdminDenunciasActivity.this,
                    AdminDetalheDenunciaActivity.class
            );

            intent.putExtra("tipo", "💡 Iluminação pública");
            intent.putExtra("cidadao", "Maria Oliveira");
            intent.putExtra("data", "25/05/2026");
            intent.putExtra("descricao", "Poste de iluminação apagado há mais de uma semana na Rua das Flores. A região fica completamente escura à noite, gerando insegurança para os moradores.");
            intent.putExtra("status", "EM ANDAMENTO");
            intent.putExtra("latitude", -23.0900);
            intent.putExtra("longitude", -47.2150);

            startActivity(intent);
        });

        // CARD 3 — CONCLUÍDO
        cardDenuncia3.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AdminDenunciasActivity.this,
                    AdminDetalheDenunciaActivity.class
            );

            intent.putExtra("tipo", "🗑️ Lixo acumulado");
            intent.putExtra("cidadao", "Carlos Santos");
            intent.putExtra("data", "20/05/2026");
            intent.putExtra("descricao", "Acúmulo de lixo no terreno baldio próximo à escola municipal. Há entulho, restos de material de construção e lixo doméstico espalhado pelo local.");
            intent.putExtra("status", "CONCLUÍDO");
            intent.putExtra("latitude", -23.0820);
            intent.putExtra("longitude", -47.2300);

            startActivity(intent);
        });
    }
}
