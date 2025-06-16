package com.example.chemodan;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class activity_weather extends AppCompatActivity {
    private RecyclerView recyclerView;
    private WeatherAdapter adapter;
    private List<WeatherItem> cities;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "WeatherPrefs";
    private static final String KEY_CITIES = "cities";
    private static final String API_KEY = "c7ce10d8043a6da450ca891e9365f4a3";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_weather);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.weather_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        recyclerView = findViewById(R.id.recycler_weather);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cities = new ArrayList<>();
        adapter = new WeatherAdapter(cities);
        recyclerView.setAdapter(adapter);

        loadSavedCities();
        fetchWeatherForAll();

        FloatingActionButton fab = findViewById(R.id.fab_add_city);
        fab.setOnClickListener(v -> showAddCityDialog());

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                String city = cities.get(pos).getCity();
                cities.remove(pos);
                saveCities();
                adapter.notifyItemRemoved(pos);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void loadSavedCities() {
        Set<String> saved = preferences.getStringSet(KEY_CITIES, null);
        if (saved == null || saved.isEmpty()) {
            saved = new HashSet<>();
            saved.add("Kazan");
            preferences.edit().putStringSet(KEY_CITIES, saved).apply();
        }
        cities.clear();
        for (String city : saved) {
            cities.add(new WeatherItem(city, 0, "-", 0, 0));
        }
        adapter.notifyDataSetChanged();
    }

    private void saveCities() {
        Set<String> set = new HashSet<>();
        for (WeatherItem item : cities) set.add(item.getCity());
        preferences.edit().putStringSet(KEY_CITIES, set).apply();
    }

    private void showAddCityDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("City name");
        new AlertDialog.Builder(this)
            .setTitle("Add city")
            .setView(input)
            .setPositiveButton("Add", (dialog, which) -> {
                String city = input.getText().toString().trim();
                if (!city.isEmpty() && !containsCity(city)) {
                    cities.add(new WeatherItem(city, 0, "-", 0, 0));
                    saveCities();
                    fetchWeatherForCity(city, cities.size() - 1);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "City already added or empty", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private boolean containsCity(String city) {
        for (WeatherItem item : cities) {
            if (item.getCity().equalsIgnoreCase(city)) return true;
        }
        return false;
    }

    private void fetchWeatherForAll() {
        for (int i = 0; i < cities.size(); i++) {
            fetchWeatherForCity(cities.get(i).getCity(), i);
        }
    }

    private void fetchWeatherForCity(String city, int index) {
        String url = String.format(BASE_URL, city, API_KEY);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    double temp = json.getJSONObject("main").getDouble("temp");
                    int humidity = json.getJSONObject("main").getInt("humidity");
                    double wind = json.getJSONObject("wind").getDouble("speed");
                    String state = "-";
                    JSONArray weatherArr = json.getJSONArray("weather");
                    if (weatherArr.length() > 0) {
                        state = weatherArr.getJSONObject(0).getString("main");
                    }
                    WeatherItem item = cities.get(index);
                    item.setTemp(temp);
                    item.setHumidity(humidity);
                    item.setWind(wind);
                    item.setState(state);
                    adapter.notifyItemChanged(index);
                } catch (JSONException e) {
                    Toast.makeText(this, "Error parsing weather for " + city, Toast.LENGTH_SHORT).show();
                }
            },
            error -> Toast.makeText(this, "Error fetching weather for " + city, Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }
}