package com.example.chemodan;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
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
import android.text.InputType;

public class WeatherManager {
    private final activity_weather activity;
    private final RecyclerView recyclerView;
    private final WeatherAdapter adapter;
    private final List<WeatherItem> cities;
    private final List<WeatherItem> allCities;
    private final SharedPreferences preferences;

    private static final String PREF_NAME = "WeatherPrefs";
    private static final String KEY_CITIES = "cities";
    private static final String API_KEY = "c7ce10d8043a6da450ca891e9365f4a3";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";

    public WeatherManager(activity_weather activity) {
        this.activity = activity;
        this.recyclerView = activity.findViewById(R.id.recycler_weather);
        this.cities = new ArrayList<>();
        this.allCities = new ArrayList<>();
        this.preferences = activity.getSharedPreferences(PREF_NAME, activity.MODE_PRIVATE);
        this.adapter = new WeatherAdapter(cities);

        setupRecyclerView();
        setupFab();
        setupSwipeToDelete();
        setupSearch();
        loadSavedCities();
        fetchWeatherForAll();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        FloatingActionButton fab = activity.findViewById(R.id.fab_add_city);
        fab.setOnClickListener(v -> showAddCityDialog());
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                String city = cities.get(pos).getCity();
                removeCity(city);
                adapter.notifyItemRemoved(pos);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void setupSearch() {
        EditText search = activity.findViewById(R.id.search_city);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterCities(s.toString());
            }
        });
    }

    private void loadSavedCities() {
        Set<String> saved = preferences.getStringSet(KEY_CITIES, null);
        if (saved == null || saved.isEmpty()) {
            saved = new HashSet<>();
            saved.add("Kazan");
            preferences.edit().putStringSet(KEY_CITIES, saved).apply();
        }
        cities.clear();
        allCities.clear();
        for (String city : saved) {
            WeatherItem item = new WeatherItem(city, 0, "-", 0, 0);
            cities.add(item);
            allCities.add(item);
        }
        adapter.notifyDataSetChanged();
    }

    private void saveCities() {
        Set<String> set = new HashSet<>();
        for (WeatherItem item : allCities) {
            set.add(item.getCity());
        }
        preferences.edit().putStringSet(KEY_CITIES, set).apply();
    }

    private void removeCity(String city) {
        for (int i = 0; i < allCities.size(); i++) {
            if (allCities.get(i).getCity().equalsIgnoreCase(city)) {
                allCities.remove(i);
                break;
            }
        }
        filterCities(((EditText)activity.findViewById(R.id.search_city)).getText().toString());
        saveCities();
    }

    private void showAddCityDialog() {
        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("City name");

        new AlertDialog.Builder(activity)
                .setTitle("Add city")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String city = input.getText().toString().trim();
                    if (!city.isEmpty() && !containsCity(city)) {
                        WeatherItem item = new WeatherItem(city, 0, "-", 0, 0);
                        allCities.add(item);
                        saveCities();
                        fetchWeatherForCity(city, allCities.size() - 1);
                        filterCities(((EditText)activity.findViewById(R.id.search_city)).getText().toString());
                    } else {
                        Toast.makeText(activity, "City already added or empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean containsCity(String city) {
        for (WeatherItem item : allCities) {
            if (item.getCity().equalsIgnoreCase(city)) {
                return true;
            }
        }
        return false;
    }

    private void filterCities(String query) {
        cities.clear();
        if (query.isEmpty()) {
            cities.addAll(allCities);
        } else {
            for (WeatherItem item : allCities) {
                if (item.getCity().toLowerCase().contains(query.toLowerCase())) {
                    cities.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchWeatherForAll() {
        for (int i = 0; i < allCities.size(); i++) {
            fetchWeatherForCity(allCities.get(i).getCity(), i);
        }
    }

    private void fetchWeatherForCity(String city, int index) {
        String url = String.format(BASE_URL, city, API_KEY);
        RequestQueue queue = Volley.newRequestQueue(activity);

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

                        WeatherItem item = allCities.get(index);
                        item.setTemp(temp);
                        item.setHumidity(humidity);
                        item.setWind(wind);
                        item.setState(state);

                        filterCities(((EditText)activity.findViewById(R.id.search_city)).getText().toString());
                    } catch (JSONException e) {
                        Toast.makeText(activity, "Error parsing weather for " + city, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(activity, "Error fetching weather for " + city, Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }
}