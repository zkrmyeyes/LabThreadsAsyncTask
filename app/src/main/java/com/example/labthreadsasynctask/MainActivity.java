package com.example.labthreadsasynctask;


import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Lab 1 — Threads Android
 *
 * Concepts démontrés :
 *  - UI Thread vs Worker Thread
 *  - view.post()  /  runOnUiThread()  /  Handler
 *  - AsyncTask (doInBackground → onProgressUpdate → onPostExecute)
 *  - Réactivité de l'interface pendant les traitements longs
 */
public class MainActivity extends AppCompatActivity {

    // ── Vues ──────────────────────────────────────────────────────────────────
    private TextView    txtStatus;
    private TextView    txtProgress;
    private ProgressBar progressBar;
    private ImageView   img;
    private Button      btnLoadThread;
    private Button      btnCalcAsync;

    // ── Cycle de vie ──────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        txtStatus    = findViewById(R.id.txtStatus);
        txtProgress  = findViewById(R.id.txtProgress);
        progressBar  = findViewById(R.id.progressBar);
        img          = findViewById(R.id.img);
        btnLoadThread = findViewById(R.id.btnLoadThread);
        btnCalcAsync  = findViewById(R.id.btnCalcAsync);

        // Connexion des boutons
        btnLoadThread.setOnClickListener(v -> lancerChargementAvecThread());
        btnCalcAsync .setOnClickListener(v -> lancerCalculAvecAsyncTask());
        findViewById(R.id.btnToast).setOnClickListener(v -> afficherToast());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BOUTON 1 — Charger image avec un Worker Thread
    // Concept : Thread + runOnUiThread() pour revenir sur l'UI Thread
    // ══════════════════════════════════════════════════════════════════════════
    private void lancerChargementAvecThread() {
        // Désactiver le bouton pendant le traitement
        btnLoadThread.setEnabled(false);
        setStatus("🔵 Thread démarré… chargement en cours (3 s)");

        // Créer et démarrer un Worker Thread
        Thread workerThread = new Thread(() -> {

            // ── Travail en arrière-plan (NON sur le UI Thread) ──────────────
            try {
                // Simulation d'un chargement (ex : réseau, disque)
                Thread.sleep(3000); // 3 secondes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // ── Retour sur le UI Thread pour modifier les vues ──────────────
            // MÉTHODE : runOnUiThread() — la plus simple dans une Activity
            runOnUiThread(() -> {
                // Changer la couleur de l'ImageView pour simuler un chargement
                img.setBackground(new ColorDrawable(Color.parseColor("#1565C0")));
                img.setImageDrawable(null);
                img.setBackgroundColor(Color.parseColor("#1565C0"));

                setStatus("✅ Image chargée ! (via runOnUiThread)");
                btnLoadThread.setEnabled(true);

                Toast.makeText(MainActivity.this,
                        "Thread → image chargée !", Toast.LENGTH_SHORT).show();
            });
        });

        workerThread.start(); // Démarrer le thread
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BOUTON 2 — Calcul lourd avec AsyncTask
    // Concept : doInBackground() → onProgressUpdate() → onPostExecute()
    // ══════════════════════════════════════════════════════════════════════════
    private void lancerCalculAvecAsyncTask() {
        new CalculLourdTask().execute();
    }

    /**
     * AsyncTask<Params, Progress, Result>
     *  - Params   : type des paramètres d'entrée  (Void = aucun)
     *  - Progress : type de la progression        (Integer = 0..100)
     *  - Result   : type du résultat final        (Long = résultat du calcul)
     */
    @SuppressWarnings("deprecation") // AsyncTask déprécié en API 30, mais conservé à titre pédagogique
    private class CalculLourdTask extends AsyncTask<Void, Integer, Long> {

        // ── Avant le traitement — s'exécute sur le UI Thread ─────────────────
        @Override
        protected void onPreExecute() {
            btnCalcAsync.setEnabled(false);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            txtProgress.setVisibility(View.VISIBLE);
            txtProgress.setText("Progression : 0%");
            setStatus("🟢 AsyncTask démarrée…");
        }

        // ── Traitement long — s'exécute dans un Worker Thread ─────────────────
        // ⚠️  Interdit de toucher aux vues ici !
        @Override
        protected Long doInBackground(Void... params) {
            long resultat = 0;

            for (int i = 0; i <= 100; i++) {
                // Simulation d'un calcul progressif
                resultat += i * i;

                // Envoyer la progression vers onProgressUpdate()
                publishProgress(i);

                try {
                    Thread.sleep(50); // pause de 50 ms par étape = 5 s total
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return resultat;
        }

        // ── Mise à jour de la progression — s'exécute sur le UI Thread ───────
        @Override
        protected void onProgressUpdate(Integer... values) {
            int pct = values[0];
            progressBar.setProgress(pct);
            txtProgress.setText("Progression : " + pct + "%");
            setStatus("⚙️ Calcul en cours… " + pct + "%");
        }

        // ── Fin du traitement — s'exécute sur le UI Thread ───────────────────
        @Override
        protected void onPostExecute(Long resultat) {
            progressBar.setProgress(100);
            txtProgress.setText("Progression : 100%");
            setStatus("✅ Calcul terminé ! Résultat = " + resultat);
            btnCalcAsync.setEnabled(true);

            Toast.makeText(MainActivity.this,
                    "AsyncTask terminée ! Résultat = " + resultat,
                    Toast.LENGTH_LONG).show();

            // Masquer la barre après 1 seconde (via Handler — 3e méthode)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                progressBar.setVisibility(View.INVISIBLE);
                txtProgress.setVisibility(View.INVISIBLE);
            }, 1500);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BOUTON 3 — Toast (test réactivité UI Thread)
    // Ce bouton DOIT répondre même pendant les traitements → preuve que l'UI
    // n'est pas bloquée (Worker Thread et AsyncTask tournent en arrière-plan)
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherToast() {
        // Toast s'exécute directement sur le UI Thread
        // Si les boutons 1 et 2 bloquaient l'UI Thread, ce Toast ne s'afficherait pas
        Toast.makeText(this,
                "✅ L'UI Thread est réactif ! Pas de blocage.",
                Toast.LENGTH_SHORT).show();
    }

    // ── Utilitaire : mettre à jour le statut ──────────────────────────────────
    private void setStatus(String message) {
        txtStatus.setText(message);
    }
}