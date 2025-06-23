package com.example.chemodan;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class activity_weather extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationMenu navigationMenu;
    private WeatherManager weatherManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_weather);

        drawerLayout = findViewById(R.id.weather_drawer_layout);

        // Инициализация меню
        navigationMenu = new NavigationMenu(this, drawerLayout);

        // Инициализация менеджера погоды
        weatherManager = new WeatherManager(this);

        // Кнопка меню
        findViewById(R.id.btn_menu_weather_bar).setOnClickListener(v ->
                navigationMenu.openDrawer()
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.weather_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}