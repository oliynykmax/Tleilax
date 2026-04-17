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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment implements com.example.tleilax.simulation.SimulationEngine.Listener {

    public enum GraphMode {
        ALL_SPECIES,
        ANIMALS_ONLY,
        PLANTS_ONLY
    }

    private static final LinkedHashSet<String> DEFAULT_ANIMALS =
            new LinkedHashSet<>(Arrays.asList("WOLF", "RABBIT", "MOUSE", "DEER"));

    private static final LinkedHashSet<String> DEFAULT_PLANTS =
            new LinkedHashSet<>(Arrays.asList("BERRY_BUSH", "TREE"));

    /** Stable color mapping — keyed by EntityType name() / PlantType name(). */
    private static final LinkedHashMap<String, Integer> SPECIES_COLORS = new LinkedHashMap<>();
    static {
        for (com.example.tleilax.model.EntityType type : com.example.tleilax.model.EntityType.values()) {
            SPECIES_COLORS.put(type.name(), type.getRenderColor());
        }
        for (com.example.tleilax.simulation.PlantType type : com.example.tleilax.simulation.PlantType.values()) {
            if (!SPECIES_COLORS.containsKey(type.name())) {
                SPECIES_COLORS.put(type.name(), type.getRenderColor());
            }
        }
    }

    private static final int FALLBACK_COLOR = Color.parseColor("#928374");

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<StatisticsSnapshot> history = new ArrayList<>();
    private final LinkedHashSet<String> knownSpecies = new LinkedHashSet<>();

    private LineChart lineChart;
    private TextView summaryText;
    private TextView chartPlaceholderText;

    private MaterialButton btnAllSpecies;
    private MaterialButton btnAnimalsOnly;
    private MaterialButton btnPlantsOnly;

    private GraphMode graphMode = GraphMode.ALL_SPECIES;
    private long lastRenderTime = 0;
    private static final long RENDER_THROTTLE_MS = 640;
    private static final int MAX_DISPLAY_HISTORY = 500;

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
        int start = Math.max(0, existingHistory.size() - MAX_DISPLAY_HISTORY);
        history.addAll(existingHistory.subList(start, existingHistory.size()));
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
        int start = Math.max(0, latestHistory.size() - MAX_DISPLAY_HISTORY);
        history.addAll(latestHistory.subList(start, latestHistory.size()));
        
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

                if (checkedId == R.id.btn_all_species) {
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
        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);

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
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(11f);
    }

    private void updateButtonStates() {
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

        for (int i = 0; i < speciesToGraph.size(); i++) {
            String species = speciesToGraph.get(i);
            ArrayList<Entry> entries = new ArrayList<>();
            
            for (StatisticsSnapshot snapshot : history) {
                entries.add(new Entry(snapshot.getTick(), getPopulation(snapshot, species)));
            }

            LineDataSet set = new LineDataSet(entries, formatSpeciesName(species));
            set.setColor(getStableColor(species));
            set.setLineWidth(2f);
            set.setDrawCircles(false);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSets.add(set);
        }

        LineData data = new LineData(dataSets);
        lineChart.setData(data);

        // Fit all data into the fixed-size chart
        lineChart.fitScreen();
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
                dominantSpecies = formatSpeciesName(species) + " (" + population + ")";
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
            case ANIMALS_ONLY: return filterKnownSpecies(DEFAULT_ANIMALS);
            case PLANTS_ONLY: return filterKnownSpecies(DEFAULT_PLANTS);
            case ALL_SPECIES:
            default: return new ArrayList<>(knownSpecies);
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

    private int getPopulation(@NonNull StatisticsSnapshot snapshot, @NonNull String species) {
        Integer value = snapshot.getPopulationBySpecies().get(species);
        return value == null ? 0 : value;
    }

    /**
        Integer color = SPECIES_COLORS.get(species);
        if (color != null) return color;

        // Try case-insensitive fallback for any variant naming
        for (Map.Entry<String, Integer> entry : SPECIES_COLORS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(species)) {
                return entry.getValue();
            }
        }
        return FALLBACK_COLOR;
    }

    /**
        // First try EntityType.displayName for a canonical label
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
              .append(parts[i].substring(1).toLowerCase(Locale.US));
        }
        return sb.toString();
    }

    private String describeGraphMode(@NonNull List<String> speciesToGraph) {
        switch (graphMode) {
            case ANIMALS_ONLY: return "showing animals only (" + speciesToGraph.size() + ")";
            case PLANTS_ONLY: return "showing plants only (" + speciesToGraph.size() + ")";
            case ALL_SPECIES:
            default: return "showing all " + speciesToGraph.size() + " species";
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
