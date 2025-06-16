package com.example.chemodan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder> {
    private List<WeatherItem> cities;

    public WeatherAdapter(List<WeatherItem> cities) {
        this.cities = cities;
    }

    @NonNull
    @Override
    public WeatherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weather, parent, false);
        return new WeatherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherViewHolder holder, int position) {
        WeatherItem item = cities.get(position);
        holder.city.setText(item.getCity());
        holder.temp.setText(String.format("%.0f°", item.getTemp()));
        holder.state.setText(item.getState());
        holder.wind.setText(String.format("%.0f km/h", item.getWind()));
        holder.humidity.setText(item.getHumidity() + " %");
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        TextView city, temp, state, wind, humidity;
        ImageView windIcon, humidityIcon;
        public WeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            city = itemView.findViewById(R.id.text_city);
            temp = itemView.findViewById(R.id.text_temp);
            state = itemView.findViewById(R.id.text_state);
            wind = itemView.findViewById(R.id.text_wind);
            humidity = itemView.findViewById(R.id.text_humidity);
        }
    }
} 