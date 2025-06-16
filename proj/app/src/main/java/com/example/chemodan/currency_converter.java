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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class currency_converter extends AppCompatActivity implements CurrencyAdapter.OnCurrencyChangeListener {
    private RecyclerView recyclerView;
    private CurrencyAdapter adapter;
    private List<СurrencyItem> currencies;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "CurrencyPrefs";
    private static final String KEY_CURRENCIES = "currencies";
    private static final String API_KEY = "YOUR_API_KEY"; // Replace with your API key
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";

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
}