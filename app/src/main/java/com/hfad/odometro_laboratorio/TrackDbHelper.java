package com.hfad.odometro_laboratorio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Persistencia (SQLite) de los recorridos para el reporte consolidado. */
public class TrackDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "odometro.db";
    private static final int DB_VERSION = 1;

    public TrackDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE recorrido (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "estudiante TEXT," +
                "fecha TEXT," +
                "distancia_m REAL," +
                "precision_m REAL," +
                "intervalo_s INTEGER," +
                "latitud REAL," +
                "longitud REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS recorrido");
        onCreate(db);
    }

    public void insertar(String estudiante, String fecha, double distancia,
                         float precision, long intervalo, double lat, double lon) {
        ContentValues v = new ContentValues();
        v.put("estudiante", estudiante);
        v.put("fecha", fecha);
        v.put("distancia_m", distancia);
        v.put("precision_m", precision);
        v.put("intervalo_s", intervalo);
        v.put("latitud", lat);
        v.put("longitud", lon);
        getWritableDatabase().insert("recorrido", null, v);
    }

    /** Reporte consolidado: lista de recorridos + total por estudiante. */
    public String reporte() {
        StringBuilder sb = new StringBuilder();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery("SELECT estudiante, fecha, distancia_m, precision_m, intervalo_s " +
                "FROM recorrido ORDER BY id DESC", null);
        while (c.moveToNext()) {
            sb.append(String.format("%s | %s | %.1f m (prec %.0f m, c/%d s)\n",
                    c.getString(0), c.getString(1), c.getDouble(2),
                    c.getDouble(3), c.getLong(4)));
        }
        c.close();

        sb.append("\n--- TOTAL POR ESTUDIANTE ---\n");
        Cursor t = db.rawQuery("SELECT estudiante, COUNT(*), SUM(distancia_m) " +
                "FROM recorrido GROUP BY estudiante", null);
        while (t.moveToNext()) {
            sb.append(String.format("%s: %d recorridos, %.1f m en total\n",
                    t.getString(0), t.getInt(1), t.getDouble(2)));
        }
        t.close();
        return sb.toString();
    }
}
