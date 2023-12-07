package com.walid.calculator;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.content.ClipData;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<String> dataList;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private int lastPosition = 1;
    public RecyclerAdapter(Context context, ArrayList<String> dataList, SharedPreferences sharedPreferences) {
        this.dataList = dataList;
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.editor = sharedPreferences.edit();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView operation;
        TextView result;
        ImageButton delete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            operation = itemView.findViewById(R.id.operationHistory);
            result = itemView.findViewById(R.id.resultHistory);
            delete = itemView.findViewById(R.id.trashBtn);
            result.setOnClickListener(view -> {
                updateSharedPrefs(result.getText().toString());
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            });
            result.setOnLongClickListener(view -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("result", result.getText());
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(context, "Copied Result", Toast.LENGTH_SHORT).show();
                return true;
            });
            operation.setOnClickListener(view -> {
                updateSharedPrefs(operation.getText().toString());
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            });
            operation.setOnLongClickListener(view -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("operation", operation.getText());
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(context, "Copied Operation", Toast.LENGTH_SHORT).show();
                return true;
            });

            delete.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    editor.remove(dataList.get(position).split("=")[0]);
                    editor.apply();
                    dataList.remove(position);
                    notifyItemRemoved(position);
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recycler_item, parent, false);
        return new RecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.operation.setText(dataList.get(position).split("=")[0]);
        holder.result.setText(dataList.get(position).split("=")[1]);
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.show_item);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }
    private void updateSharedPrefs(String textToAdd) {
        String allInputsArr = sharedPreferences.getString("allInputsArr", "");
        String allInputsText = sharedPreferences.getString("allInputsText", "");
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> allInputs = new Gson().fromJson(allInputsArr, listType);
        if (!allInputs.isEmpty() && allInputs.get(allInputs.size() - 1).contains(".")) {
            allInputs.add("*");
            allInputs.add(textToAdd);
            allInputsText += "*";
            allInputsText += textToAdd;
            String json = new Gson().toJson(allInputs);
            editor.putString("allInputsArr", json);
            editor.putString("allInputsText", allInputsText);
        } else {
            allInputs.add(textToAdd);
            allInputsText += textToAdd;
            String json = new Gson().toJson(allInputs);
            editor.putString("allInputsArr", json);
            editor.putString("allInputsText", allInputsText);
        }
        editor.apply();
    }
    @Override
    public int getItemCount() {
        return dataList.size();
    }
}

