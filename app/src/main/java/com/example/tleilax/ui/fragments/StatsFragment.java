package com.example.tleilax.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.tleilax.R;
import com.example.tleilax.utils.StatisticsSnapshot;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.button.MaterialButton;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment implements com.example.tleilax.simulation.SimulationEngine.Listener {

    public enum GraphMode {
        TOP_CURRENT,
        ALL_SPECIES,
        ANIMALS_ONLY,
        PLANTS_ONLY
    }

    private static final LinkedHashSet<String> DEFAULT_ANIMALS =
            new LinkedHashSet<>(Arrays.asList("WOLF", "RABBIT", "MOUSE", "DEER", "Wolf", "Rabbit", "Mouse", "Deer"));

    private static final LinkedHashSet<String> DEFAULT_PLANTS =
            new LinkedHashSet<>(Arrays.asList("BERRY_BUSH", "TREE", "Berry Bush", "Tree", "BerryBush"));

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<StatisticsSnapshot> history = new ArrayList<>();
    private final LinkedHashSet<String> knownSpecies = new LinkedHashSet<>();

    private LineChart lineChart;
    private TextView summaryText;
    private TextView chartPlaceholderText;

    private MaterialButton btnTopSpecies;
    private MaterialButton btnAllSpecies;
    private MaterialButton btnAnimalsOnly;
    private MaterialButton btnPlantsOnly;

    private GraphMode graphMode = GraphMode.TOP_CURRENT;
    private int maxSeries = 5;
    private long lastRenderTime = 0;
    private static final long RENDER_THROTTLE_MS = 640;

    public StatsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupToggleButtons(view);
        setupChartStyle();
        updateButtonStates();
        
        List<StatisticsSnapshot> existingHistory = 
                com.example.tleilax.utils.StatTracker.getInstance().getHistory();
        history.clear();
        knownSpecies.clear();
        history.addAll(existingHistory);
        for (StatisticsSnapshot snapshot : history) {
            knownSpecies.addAll(snapshot.getPopulationBySpecies().keySet());
        }
        
        renderAll();

        com.example.tleilax.simulation.SimulationSession.getEngine().addListener(this);
    }

    @Override
    public void onDestroyView() {
        com.example.tleilax.simulation.SimulationSession.getEngine().removeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onWorldUpdated(@NonNull com.example.tleilax.simulation.WorldSnapshot snapshot) {
        long now = System.currentTimeMillis();
        
        List<StatisticsSnapshot> latestHistory = 
                com.example.tleilax.utils.StatTracker.getInstance().getHistory();
        
        history.clear();
        knownSpecies.clear(); 
        
        if (latestHistory.size() > 100) {
            history.addAll(latestHistory.subList(latestHistory.size() - 100, latestHistory.size()));
        } else {
            history.addAll(latestHistory);
        }
        
        for (StatisticsSnapshot s : history) {
            knownSpecies.addAll(s.getPopulationBySpecies().keySet());
        }
        
        if (now - lastRenderTime >= RENDER_THROTTLE_MS) {
            mainHandler.post(this::renderAll);
        } else {
            mainHandler.post(this::renderSummary);
        }
    }

    public void setGraphMode(@NonNull GraphMode mode) {
        if (graphMode == mode) return;
        graphMode = mode;
        updateButtonStates();
        renderAll();
    }

    private void bindViews(@NonNull View root) {
        lineChart = root.findViewById(R.id.line_chart);
        summaryText = root.findViewById(R.id.text_summary);
        chartPlaceholderText = root.findViewById(R.id.text_chart_placeholder);

        btnTopSpecies = root.findViewById(R.id.btn_top_species);
        btnAllSpecies = root.findViewById(R.id.btn_all_species);
        btnAnimalsOnly = root.findViewById(R.id.btn_animals_only);
        btnPlantsOnly = root.findViewById(R.id.btn_plants_only);
    }

    private void setupToggleButtons(View root) {
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroup = 
                root.findViewById(R.id.graph_toggle_group);
        
        if (toggleGroup != null) {
            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                
                if (checkedId == R.id.btn_top_species) {
                    setGraphMode(GraphMode.TOP_CURRENT);
                } else if (checkedId == R.id.btn_all_species) {
                    setGraphMode(GraphMode.ALL_SPECIES);
                } else if (checkedId == R.id.btn_animals_only) {
                    setGraphMode(GraphMode.ANIMALS_ONLY);
                } else if (checkedId == R.id.btn_plants_only) {
                    setGraphMode(GraphMode.PLANTS_ONLY);
                }
            });
        }
    }

    private void setupChartStyle() {
        if (lineChart == null) return;

        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        int textMuted = ContextCompat.getColor(requireContext(), R.color.text_muted);
        int gridColor = Color.parseColor("#3c3836");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(textMuted);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(gridColor);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(textMuted);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(gridColor);
        leftAxis.setAxisMinimum(0f);

        lineChart.getAxisRight().setEnabled(false);

        Legend legend = lineChart.getLegend();
        legend.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void updateButtonStates() {
        updateSingleButtonStyle(btnTopSpecies, graphMode == GraphMode.TOP_CURRENT);
        updateSingleButtonStyle(btnAllSpecies, graphMode == GraphMode.ALL_SPECIES);
        updateSingleButtonStyle(btnAnimalsOnly, graphMode == GraphMode.ANIMALS_ONLY);
        updateSingleButtonStyle(btnPlantsOnly, graphMode == GraphMode.PLANTS_ONLY);
    }

    private void updateSingleButtonStyle(@Nullable MaterialButton button, boolean selected) {
        if (button == null) return;

        int gold = ContextCompat.getColor(requireContext(), R.color.accent_gold);
        button.setTextColor(gold);
        button.setStrokeColorResource(R.color.border_subtle);

        if (selected) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#33D79921")));
        } else {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.bg_surface)));
        }
    }

    private void renderAll() {
        lastRenderTime = System.currentTimeMillis();
        renderChart();
        renderSummary();
    }

    private void renderChart() {
        if (lineChart == null || !isAdded()) return;

        List<String> speciesToGraph = getSpeciesToGraph();
        boolean hasData = !history.isEmpty() && !speciesToGraph.isEmpty();

        if (!hasData) {
            lineChart.setVisibility(View.GONE);
            if (chartPlaceholderText != null) chartPlaceholderText.setVisibility(View.VISIBLE);
            return;
        }

        if (chartPlaceholderText != null) chartPlaceholderText.setVisibility(View.GONE);
        lineChart.setVisibility(View.VISIBLE);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        int[] colors = {
            Color.parseColor("#fb4934"), // red
            Color.parseColor("#b8bb26"), // green
            Color.parseColor("#fabd2f"), // yellow
            Color.parseColor("#83a598"), // blue
            Color.parseColor("#d3869b"), // purple
            Color.parseColor("#8ec07c"), // aqua
            Color.parseColor("#fe8019")  // orange
        };

        for (int i = 0; i < speciesToGraph.size(); i++) {
            String species = speciesToGraph.get(i);
            ArrayList<Entry> entries = new ArrayList<>();
            
            for (StatisticsSnapshot snapshot : history) {
                entries.add(new Entry(snapshot.getTick(), getPopulation(snapshot, species)));
            }

            LineDataSet set = new LineDataSet(entries, species);
            set.setColor(colors[i % colors.length]);
            set.setLineWidth(2f);
            set.setDrawCircles(false);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSets.add(set);
        }

        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
    }

    private void renderSummary() {
        if (summaryText == null || history.isEmpty()) return;

        StatisticsSnapshot latest = history.get(history.size() - 1);
        int aliveSpecies = 0;
        int extinctSpecies = 0;
        String dominantSpecies = "None";
        int maxPopulationValue = -1;

        for (String species : knownSpecies) {
            int population = getPopulation(latest, species);
            if (population > 0) aliveSpecies++; else extinctSpecies++;

            if (population > maxPopulationValue) {
                maxPopulationValue = population;
                dominantSpecies = species + " (" + population + ")";
            }
        }

        List<String> speciesToGraph = getSpeciesToGraph();
        String summary = String.format(Locale.US,
                "Tick %d · Population %d · Grass %.1f%%\nAlive species: %d · Extinct: %d\nDominant: %s · Status: %s\nGraph: %s",
                latest.getTick(), latest.getTotalPopulation(), latest.getGrassCoverage() * 100,
                aliveSpecies, extinctSpecies, dominantSpecies, resolveStatus(latest), describeGraphMode(speciesToGraph));

        summaryText.setText(summary);
    }

    private List<String> getSpeciesToGraph() {
        switch (graphMode) {
            case ALL_SPECIES: return new ArrayList<>(knownSpecies);
            case ANIMALS_ONLY: return filterKnownSpecies(DEFAULT_ANIMALS);
            case PLANTS_ONLY: return filterKnownSpecies(DEFAULT_PLANTS);
            case TOP_CURRENT:
            default: return getTopSpeciesByCurrentPopulation(maxSeries);
        }
    }

    private List<String> filterKnownSpecies(@NonNull LinkedHashSet<String> allowedSpecies) {
        List<String> result = new ArrayList<>();
        for (String species : knownSpecies) {
            for (String allowed : allowedSpecies) {
                if (allowed.equalsIgnoreCase(species)) {
                    result.add(species);
                    break;
                }
            }
        }
        return result;
    }

    private List<String> getTopSpeciesByCurrentPopulation(int limit) {
        if (history.isEmpty()) return new ArrayList<>();
        StatisticsSnapshot latest = history.get(history.size() - 1);
        List<Map.Entry<String, Integer>> populations = new ArrayList<>();

        for (String species : knownSpecies) {
            if (species.equalsIgnoreCase("GRASS")) continue;
            populations.add(new AbstractMap.SimpleEntry<>(species, getPopulation(latest, species)));
        }

        populations.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> result = new ArrayList<>();
        int count = Math.min(Math.max(1, limit), populations.size());
        for (int i = 0; i < count; i++) result.add(populations.get(i).getKey());
        return result;
    }

    private int getPopulation(@NonNull StatisticsSnapshot snapshot, @NonNull String species) {
        Integer value = snapshot.getPopulationBySpecies().get(species);
        return value == null ? 0 : value;
    }

    private String describeGraphMode(@NonNull List<String> speciesToGraph) {
        switch (graphMode) {
            case ALL_SPECIES: return "showing all " + speciesToGraph.size() + " species";
            case ANIMALS_ONLY: return "showing animals only (" + speciesToGraph.size() + ")";
            case PLANTS_ONLY: return "showing plants only (" + speciesToGraph.size() + ")";
            case TOP_CURRENT:
            default: return "showing top " + speciesToGraph.size() + " by current population";
        }
    }

    private String resolveStatus(@NonNull StatisticsSnapshot latest) {
        if (history.size() < 2) return "Starting";
        int prev = history.get(history.size() - 2).getTotalPopulation();
        int curr = latest.getTotalPopulation();
        if (curr == 0) return "Extinct";
        if (prev == 0) return "Recovering";
        int delta = curr - prev;
        int threshold = Math.max(2, (int) Math.ceil(prev * 0.10));
        if (delta >= threshold) return "Growing";
        if (delta <= -threshold) return "Declining";
        return "Stable";
    }
}
