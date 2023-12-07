package com.walid.calculator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.Map;

public class HistoryViewer extends AppCompatActivity {

    private RecyclerAdapter adapter;
    private RecyclerView recyclerView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String SHARED_NAME = "History";
    private ArrayList<String> allHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_viewer);
        sharedPreferences = getSharedPreferences(SHARED_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        allHistory = new ArrayList<>();
        ImageButton backbtn = findViewById(R.id.backBtn);
        backbtn.setOnClickListener(view -> {
            Intent intent = new Intent(HistoryViewer.this, MainActivity.class);
            startActivity(intent);
        });
        Map<String, ?> allData = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!key.equals("night") && !key.equals("allInputsArr") && !key.equals("allInputsText")){
            allHistory.add(key + "=" + value.toString());
        }}
        adapter = new RecyclerAdapter(this, allHistory, sharedPreferences);
        recyclerView = findViewById(R.id.historyList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void ClearAll(View view) {
        for (String key : sharedPreferences.getAll().keySet()) {
            if (!key.equals("night")) {
                editor.remove(key);
            }
        }
        editor.apply();
        Intent intent = new Intent(HistoryViewer.this, MainActivity.class);
        startActivity(intent);
    }
}
