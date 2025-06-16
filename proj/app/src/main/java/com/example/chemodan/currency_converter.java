package com.example.chemodan;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.view.View;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class currency_converter extends AppCompatActivity implements CurrencyAdapter.OnCurrencyChangeListener {
    private RecyclerView recyclerView;
    private CurrencyAdapter adapter;
    private List<СurrencyItem> currencies;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "CurrencyPrefs";
    private static final String KEY_CURRENCIES = "currencies";
    private static final String API_KEY = "5594bb7d6b18e1c5968bb3e4";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final String[] ALL_CURRENCIES = {"USD", "EUR", "JPY", "GBP", "CNY", "RUB", "AUD", "CAD", "CHF", "SEK", "NZD", "SGD", "HKD", "NOK", "KRW", "TRY", "INR", "BRL", "ZAR"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_currency_converter);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        recyclerView = findViewById(R.id.recycler_currencies);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        currencies = new ArrayList<>();
        adapter = new CurrencyAdapter(currencies, this);
        recyclerView.setAdapter(adapter);

        loadSavedCurrencies();
        fetchExchangeRates();

        FloatingActionButton fab = findViewById(R.id.fab_add_currency);
        fab.setOnClickListener(v -> showAddCurrencyDialog());

        // Swipe to delete (кроме RUB и USD)
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                String code = currencies.get(pos).getCode();
                if (code.equals("RUB") || code.equals("USD")) {
                    adapter.notifyItemChanged(pos);
                    Toast.makeText(currency_converter.this, "RUB и USD нельзя удалить", Toast.LENGTH_SHORT).show();
                } else {
                    currencies.remove(pos);
                    saveCurrencies();
                    adapter.notifyItemRemoved(pos);
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void loadSavedCurrencies() {
        String savedCurrencies = preferences.getString(KEY_CURRENCIES, "RUB,USD");
        String[] currencyCodes = savedCurrencies.split(",");
        
        for (int i = 0; i < currencyCodes.length; i++) {
            currencies.add(new СurrencyItem(
                currencyCodes[i],
                1.0, // Will be updated with actual rates
                0.0,
                i == 0 // First currency is base
            ));
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchExchangeRates() {
        if (currencies.isEmpty()) return;
        
        String baseCurrency = currencies.get(0).getCode();
        String url = BASE_URL + API_KEY + "/latest/" + baseCurrency;

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject rates = jsonResponse.getJSONObject("conversion_rates");
                    
                    for (СurrencyItem currency : currencies) {
                        if (!currency.isBase()) {
                            double rate = rates.getDouble(currency.getCode());
                            currency.setRate(rate);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    Toast.makeText(this, "Error parsing exchange rates", Toast.LENGTH_SHORT).show();
                }
            },
            error -> Toast.makeText(this, "Error fetching exchange rates", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

    @Override
    public void onCurrencyValueChanged(СurrencyItem baseCurrency) {
        adapter.updateValues(baseCurrency);
    }

    private void showAddCurrencyDialog() {
        // Список только тех валют, которых ещё нет
        List<String> available = new ArrayList<>();
        for (String code : ALL_CURRENCIES) {
            boolean exists = false;
            for (СurrencyItem item : currencies) {
                if (item.getCode().equals(code)) { exists = true; break; }
            }
            if (!exists) available.add(code);
        }
        if (available.isEmpty()) {
            Toast.makeText(this, "Все валюты уже добавлены", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] arr = available.toArray(new String[0]);
        new AlertDialog.Builder(this)
            .setTitle("Выберите валюту")
            .setItems(arr, (dialog, which) -> {
                String selected = arr[which];
                currencies.add(new СurrencyItem(selected, 1.0, 0.0, false));
                saveCurrencies();
                fetchExchangeRates();
                adapter.notifyDataSetChanged();
            })
            .show();
    }

    private void saveCurrencies() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currencies.size(); i++) {
            sb.append(currencies.get(i).getCode());
            if (i < currencies.size() - 1) sb.append(",");
        }
        preferences.edit().putString(KEY_CURRENCIES, sb.toString()).apply();
    }
}