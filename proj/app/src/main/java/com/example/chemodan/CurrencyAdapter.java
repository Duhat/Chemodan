package com.example.chemodan;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder> {

    private List<СurrencyItem> currencies;
    private OnCurrencyChangeListener listener;
    private int focusedIndex = -1;

    public interface OnCurrencyChangeListener {
        void onCurrencyValueChanged(СurrencyItem baseCurrency);
    }

    public CurrencyAdapter(List<СurrencyItem> currencies, OnCurrencyChangeListener listener) {
        this.currencies = currencies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CurrencyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_currency, parent, false);
        return new CurrencyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CurrencyViewHolder holder, int position) {
        СurrencyItem item = currencies.get(position);
        holder.codeText.setText(item.getCode());
        holder.inputValue.setText(String.valueOf(item.getValue()));

        // Load flag using flagcdn.com
        String flagUrl = "https://flagcdn.com/w80/" + item.getCode().substring(0, 2).toLowerCase() + ".png";
        Glide.with(holder.flag.getContext())
                .load(flagUrl)
                .into(holder.flag);

        holder.inputValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                focusedIndex = holder.getAdapterPosition();
            }
        });

        holder.inputValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (holder.getAdapterPosition() == focusedIndex) {
                    try {
                        double value = Double.parseDouble(s.toString());
                        СurrencyItem base = currencies.get(focusedIndex);
                        base.setValue(value);
                        listener.onCurrencyValueChanged(base);
                    } catch (NumberFormatException ignored) {}
                }
            }
        });
    }

    public void updateValues(СurrencyItem base) {
        for (int i = 0; i < currencies.size(); i++) {
            СurrencyItem item = currencies.get(i);
            if (i != focusedIndex) {
                double newValue = base.getValue() * (item.getRate() / base.getRate());
                item.setValue(newValue);
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    static class CurrencyViewHolder extends RecyclerView.ViewHolder {
        ImageView flag;
        TextView codeText;
        EditText inputValue;

        public CurrencyViewHolder(@NonNull View itemView) {
            super(itemView);
            flag = itemView.findViewById(R.id.image_flag);
            codeText = itemView.findViewById(R.id.text_code);
            inputValue = itemView.findViewById(R.id.input_value);
        }
    }
}
