package com.hfad.odometro_laboratorio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 698;

    private OdometerService odometer;
    private boolean bound = false;
    private TrackDbHelper db;

    private EditText etEstudiante, etPrecision, etSegundos;
    private TextView tvUbicacion, tvDistancia, tvReporte;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            odometer = ((OdometerService.OdometerBinder) binder).getOdometer();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new TrackDbHelper(this);
        etEstudiante = findViewById(R.id.etEstudiante);
        etPrecision = findViewById(R.id.etPrecision);
        etSegundos = findViewById(R.id.etSegundos);
        tvUbicacion = findViewById(R.id.tvUbicacion);
        tvDistancia = findViewById(R.id.tvDistancia);
        tvReporte = findViewById(R.id.tvReporte);

        Button btnIniciar = findViewById(R.id.btnIniciar);
        Button btnDetener = findViewById(R.id.btnDetener);
        Button btnReporte = findViewById(R.id.btnReporte);

        btnIniciar.setOnClickListener(v -> iniciar());
        btnDetener.setOnClickListener(v -> detenerYGuardar());
        btnReporte.setOnClickListener(v -> tvReporte.setText(db.reporte()));

        displayDistance();
    }

    private void iniciar() {
        if (!bound) {
            Toast.makeText(this, "Servicio no enlazado (¿permiso de ubicación?)",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        float precision;
        long segundos;
        try {
            precision = Float.parseFloat(etPrecision.getText().toString().trim());
            segundos = Long.parseLong(etSegundos.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ingrese precisión (m) y tiempo (s) válidos",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        odometer.resetDistance();
        if (!odometer.startTracking(segundos, precision)) {
            Toast.makeText(this, "No se pudo iniciar: revise permiso y GPS",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void detenerYGuardar() {
        if (!bound || !odometer.isTracking()) return;
        odometer.stopTracking();

        Location loc = odometer.getCurrentLocation();
        String estudiante = etEstudiante.getText().toString().trim();
        if (estudiante.isEmpty()) estudiante = "sin_nombre";
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());

        db.insertar(estudiante, fecha, odometer.getDistance(),
                Float.parseFloat(etPrecision.getText().toString().trim()),
                Long.parseLong(etSegundos.getText().toString().trim()),
                loc != null ? loc.getLatitude() : 0,
                loc != null ? loc.getLongitude() : 0);

        Toast.makeText(this, "Recorrido guardado", Toast.LENGTH_SHORT).show();
        tvReporte.setText(db.reporte());
    }

    /** Actualiza la UI cada segundo con los datos del servicio. */
    private void displayDistance() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                double distance = 0.0;
                if (bound && odometer != null) {
                    distance = odometer.getDistance();
                    Location loc = odometer.getCurrentLocation();
                    if (loc != null) {
                        tvUbicacion.setText(String.format(Locale.getDefault(),
                                "Ubicación actual: %.6f, %.6f",
                                loc.getLatitude(), loc.getLongitude()));
                    }
                }
                tvDistancia.setText(String.format(Locale.getDefault(),
                        "%,.1f metros", distance));
                handler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{OdometerService.PERMISSION_STRING,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            bindOdometer();
        }
    }

    private void bindOdometer() {
        Intent intent = new Intent(this, OdometerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bindOdometer();
            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}
