package com.wzjing.videoutil;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private AlertDialog errorDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.shotBtn).setOnClickListener(v->startActivity(new Intent(this, ShotActivity.class)));
        findViewById(R.id.videoBtn).setOnClickListener(v->startActivity(new Intent(this, EditActivity.class)));
        findViewById(R.id.streamBtn).setOnClickListener(v->startActivity(new Intent(this, StreamActivity.class)));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showDialog(String title, String detail) {
        if (errorDialog == null) {
            errorDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(detail)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create();
        }
        if (!errorDialog.isShowing()) {
            errorDialog.show();
        }
    }

}
