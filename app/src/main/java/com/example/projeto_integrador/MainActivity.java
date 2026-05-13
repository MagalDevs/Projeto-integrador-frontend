package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MapEventsOverlay;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private MaterialButton buttonAddDenuncia;

    private Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // obrigatório pro osmdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Insets
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

        // MAPA
        map = findViewById(R.id.map);

        map.setMultiTouchControls(true);

        GeoPoint indaiatuba = new GeoPoint(-23.0882, -47.2234);

        map.getController().setZoom(15.0);
        map.getController().setCenter(indaiatuba);

        // MARCADOR
        marker = new Marker(map);

        marker.setPosition(indaiatuba);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Local da denúncia");

        map.getOverlays().add(marker);

        // EVENTO DE CLIQUE NO MAPA
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {

            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                // move marcador
                marker.setPosition(p);

                // atualiza título
                marker.setTitle(
                        "Lat: " + p.getLatitude()
                                + "\nLng: " + p.getLongitude()
                );

                // redesenha mapa
                map.invalidate();

                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        MapEventsOverlay eventsOverlay =
                new MapEventsOverlay(mapEventsReceiver);

        map.getOverlays().add(eventsOverlay);

        // BOTÃO
        buttonAddDenuncia = findViewById(R.id.button);

        buttonAddDenuncia.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    NovaDenunciaActivity.class
            );

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}