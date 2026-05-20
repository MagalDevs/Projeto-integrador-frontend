package com.example.projeto_integrador;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DenunciasActivity extends AppCompatActivity {

    private ImageButton buttonBack;

    private LinearLayout buttonExpandir;
    private LinearLayout layoutExpandido;

    private ImageView iconExpandir;

    private boolean expandido = false;

    private MapView mapMini;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_denuncias);

        // TEXTO ENDEREÇO
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView textEndereco = findViewById(R.id.textEndereco);

        // BOTÃO VOLTAR
        buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> finish());

        // EXPANSÃO
        buttonExpandir = findViewById(R.id.buttonExpandir);
        layoutExpandido = findViewById(R.id.layoutExpandido);
        iconExpandir = findViewById(R.id.iconExpandir);

        buttonExpandir.setOnClickListener(v -> {

            expandido = !expandido;

            if (expandido) {

                layoutExpandido.setVisibility(View.VISIBLE);

                iconExpandir.setRotation(180);

            } else {

                layoutExpandido.setVisibility(View.GONE);

                iconExpandir.setRotation(0);
            }
        });

        // MINI MAPA
        mapMini = findViewById(R.id.mapMini);

        mapMini.setMultiTouchControls(false);
        mapMini.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_MOVE);
        double lat = -23.0856931;
        double lng = -47.2022082;

        String endereco = obterEndereco(lat, lng);

        textEndereco.setText(endereco);

        GeoPoint local = new GeoPoint(lat, lng);

        mapMini.getController().setZoom(16.0);
        mapMini.getController().setCenter(local);

        Marker marker = new Marker(mapMini);

        marker.setPosition(local);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapMini.getOverlays().add(marker);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapMini != null) {
            mapMini.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mapMini != null) {
            mapMini.onPause();
        }
    }

    private String obterEndereco(double latitude, double longitude) {

        try {

            Geocoder geocoder = new Geocoder(
                    this,
                    Locale.getDefault()
            );

            List<Address> enderecos =
                    geocoder.getFromLocation(latitude, longitude, 1);

            if (enderecos != null && !enderecos.isEmpty()) {

                Address endereco = enderecos.get(0);

                return endereco.getAddressLine(0);
            }

        } catch (IOException e) {

            e.printStackTrace();
        }

        return "Endereço não encontrado";
    }
}