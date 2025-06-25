package com.example.chemodan;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class WorldTimeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationMenu navigationMenu;  // Если есть
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;

    private LinearLayout timezonesContainer;

    // Пример часовых поясов, которые покажем
    private final String[] timeZoneIds = {
            "UTC",
            "Europe/Moscow",
            "America/New_York",
            "Asia/Tokyo",
            "Australia/Sydney"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_world_time);

        drawerLayout = findViewById(R.id.world_time_drawer_layout);

        navigationMenu = new NavigationMenu(this, drawerLayout);

        Button btnMenu = findViewById(R.id.btn_menu_world_time_bar);
        btnMenu.setOnClickListener(v -> navigationMenu.openDrawer());

        timezonesContainer = findViewById(R.id.timezones_container);

        // Обработка отступов для EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.world_time_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Создаем TextView для каждого часового пояса
        for (String tzId : timeZoneIds) {
            TextView tv = new TextView(this);
            tv.setTextSize(20);
            tv.setPadding(0, 10, 0, 10);
            tv.setTag(tzId); // сохраним id для обновления времени
            timezonesContainer.addView(tv);
        }

        startUpdatingTime();
    }

    private void startUpdatingTime() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimes();
                timeHandler.postDelayed(this, 60000);
            }
        };
        timeRunnable.run();
    }

    private void updateTimes() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();

        for (int i = 0; i < timezonesContainer.getChildCount(); i++) {
            TextView tv = (TextView) timezonesContainer.getChildAt(i);
            String tzId = (String) tv.getTag();

            TimeZone tz = TimeZone.getTimeZone(tzId);
            sdf.setTimeZone(tz);

            String timeStr = sdf.format(now);
            tv.setText(tzId + ": " + timeStr);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeHandler.removeCallbacks(timeRunnable);
    }
}
