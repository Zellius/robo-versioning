package ru.solodovnikov.roboversioning.example;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView textView = findViewById(R.id.text);
        try {
            final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            textView.setText(BuildConfig.VERSION_CODE+"/"+BuildConfig.VERSION_NAME+"...."+info.versionCode+"/"+info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}
