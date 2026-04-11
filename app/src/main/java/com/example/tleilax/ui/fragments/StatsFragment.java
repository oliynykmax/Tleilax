package com.example.tleilax.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.example.tleilax.R;
import com.example.tleilax.utils.StatisticsSnapshot;
import com.google.android.material.button.MaterialButton;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    public enum GraphMode {
        TOP_CURRENT,
        ALL_SPECIES,
        ANIMALS_ONLY,
        PLANTS_ONLY
    }

    private static final LinkedHashSet<String> DEFAULT_ANIMALS =
            new LinkedHashSet<>(Arrays.asList("Wolf", "Rabbit", "Mouse", "Deer"));

    private static final LinkedHashSet<String> DEFAULT_PLANTS =
            new LinkedHashSet<>(Arrays.asList("Grass", "BerryBush", "Tree"));

    private static final String ACCENT_YELLOW = "#E7C76A";
    private static final String GRID_MAJOR = "#2F3444";
    private static final String GRID_MINOR = "#252938";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<StatisticsSnapshot> history = new ArrayList<>();
    private final LinkedHashSet<String> knownSpecies = new LinkedHashSet<>();

    private FrameLayout chartContainer;
    private TextView summaryText;
    private TextView chartPlaceholderText;

    private MaterialButton btnTopSpecies;
    private MaterialButton btnAllSpecies;
    private MaterialButton btnAnimalsOnly;
    private MaterialButton btnPlantsOnly;

    private AnyChartView anyChartView;

    private GraphMode graphMode = GraphMode.TOP_CURRENT;
    private int maxSeries = 5;

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
        setupToggleButtons();
        updateButtonStates();
        renderAll();

        // Demo data for testing.
        // Remove this later when real simulation data is connected.
        // loadDemoData();
    }

    public void submitSnapshot(@NonNull StatisticsSnapshot snapshot) {
        mainHandler.post(() -> {
            upsertSnapshot(snapshot);
            renderAll();
        });
    }

    public void clearStatistics() {
        mainHandler.post(() -> {
            history.clear();
            knownSpecies.clear();
            renderAll();
        });
    }

    public void setGraphMode(@NonNull GraphMode mode) {
        mainHandler.post(() -> {
            graphMode = mode;
            updateButtonStates();
            renderAll();
        });
    }

    public void setMaxSeries(int maxSeries) {
        mainHandler.post(() -> {
            this.maxSeries = Math.max(1, maxSeries);
            renderAll();
        });
    }

    public void loadDemoData() {
        mainHandler.post(() -> {
            history.clear();
            knownSpecies.clear();

            addDemoSnapshot(0,  3, 18, 22,  8, 45, 16, 10);
            addDemoSnapshot(5,  4, 21, 25,  9, 50, 18, 10);
            addDemoSnapshot(10, 5, 24, 28, 10, 55, 20, 11);
            addDemoSnapshot(15, 6, 20, 24,  9, 51, 19, 11);
            addDemoSnapshot(20, 7, 17, 21,  8, 46, 17, 11);
            addDemoSnapshot(25, 8, 14, 18,  7, 40, 15, 12);
            addDemoSnapshot(30, 7, 13, 16,  6, 36, 14, 12);
            addDemoSnapshot(35, 6, 15, 18,  7, 39, 15, 12);
            addDemoSnapshot(40, 5, 18, 20,  8, 44, 17, 13);
            addDemoSnapshot(45, 4, 22, 23,  9, 49, 19, 13);
            addDemoSnapshot(50, 4, 25, 26, 10, 54, 21, 14);

            renderAll();
        });
    }

    private void addDemoSnapshot(
            int tick,
            int wolves,
            int rabbits,
            int mice,
            int deer,
            int grass,
            int berryBush,
            int tree
    ) {
        java.util.LinkedHashMap<String, Integer> values = new java.util.LinkedHashMap<>();
        values.put("Wolf", wolves);
        values.put("Rabbit", rabbits);
        values.put("Mouse", mice);
        values.put("Deer", deer);
        values.put("Grass", grass);
        values.put("BerryBush", berryBush);
        values.put("Tree", tree);

        upsertSnapshot(new StatisticsSnapshot(tick, values));
    }

    private void bindViews(@NonNull View root) {
        chartContainer = root.findViewById(R.id.any_chart_container);
        summaryText = root.findViewById(R.id.text_summary);

        btnTopSpecies = root.findViewById(R.id.btn_top_species);
        btnAllSpecies = root.findViewById(R.id.btn_all_species);
        btnAnimalsOnly = root.findViewById(R.id.btn_animals_only);
        btnPlantsOnly = root.findViewById(R.id.btn_plants_only);

        if (chartContainer != null) {
            for (int i = 0; i < chartContainer.getChildCount(); i++) {
                View child = chartContainer.getChildAt(i);
                if (child instanceof TextView) {
                    chartPlaceholderText = (TextView) child;
                    break;
                }
            }
        }
    }

    private void setupToggleButtons() {
        if (btnTopSpecies != null) {
            btnTopSpecies.setOnClickListener(v -> setGraphMode(GraphMode.TOP_CURRENT));
        }

        if (btnAllSpecies != null) {
            btnAllSpecies.setOnClickListener(v -> setGraphMode(GraphMode.ALL_SPECIES));
        }

        if (btnAnimalsOnly != null) {
            btnAnimalsOnly.setOnClickListener(v -> setGraphMode(GraphMode.ANIMALS_ONLY));
        }

        if (btnPlantsOnly != null) {
            btnPlantsOnly.setOnClickListener(v -> setGraphMode(GraphMode.PLANTS_ONLY));
        }
    }

    private void updateButtonStates() {
        updateSingleButtonStyle(btnTopSpecies, graphMode == GraphMode.TOP_CURRENT);
        updateSingleButtonStyle(btnAllSpecies, graphMode == GraphMode.ALL_SPECIES);
        updateSingleButtonStyle(btnAnimalsOnly, graphMode == GraphMode.ANIMALS_ONLY);
        updateSingleButtonStyle(btnPlantsOnly, graphMode == GraphMode.PLANTS_ONLY);
    }

    private void updateSingleButtonStyle(@Nullable MaterialButton button, boolean selected) {
        if (button == null) {
            return;
        }

        button.setTextColor(Color.parseColor(ACCENT_YELLOW));
        button.setStrokeColorResource(R.color.border_subtle);

        if (selected) {
            button.setBackgroundColor(Color.parseColor("#33E7C76A"));
            button.setStrokeColorResource(R.color.border_subtle);
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_surface));
            button.setStrokeColorResource(R.color.border_subtle);
        }
    }

    private void upsertSnapshot(@NonNull StatisticsSnapshot snapshot) {
        int insertIndex = history.size();

        for (int i = 0; i < history.size(); i++) {
            int existingTick = history.get(i).getTick();

            if (existingTick == snapshot.getTick()) {
                history.set(i, snapshot);
                knownSpecies.addAll(snapshot.getPopulationBySpecies().keySet());
                return;
            }

            if (existingTick > snapshot.getTick()) {
                insertIndex = i;
                break;
            }
        }

        history.add(insertIndex, snapshot);
        knownSpecies.addAll(snapshot.getPopulationBySpecies().keySet());
    }

    private void renderAll() {
        renderChart();
        renderSummary();
    }

    private void renderChart() {
        if (chartContainer == null) {
            return;
        }

        List<String> speciesToGraph = getSpeciesToGraph();
        boolean hasData = !history.isEmpty() && !speciesToGraph.isEmpty();

        if (!hasData) {
            removeChartView();
            if (chartPlaceholderText != null) {
                chartPlaceholderText.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (chartPlaceholderText != null) {
            chartPlaceholderText.setVisibility(View.GONE);
        }

        recreateChartView();

        List<DataEntry> dataEntries = new ArrayList<>();
        for (StatisticsSnapshot snapshot : history) {
            dataEntries.add(new ChartDataEntry(
                    snapshot.getTick(),
                    speciesToGraph,
                    snapshot.getPopulationBySpecies()
            ));
        }

        Cartesian cartesian = AnyChart.line();
        cartesian.animation(false);
        cartesian.padding(12d, 12d, 12d, 12d);
        cartesian.title(buildChartTitle(speciesToGraph));
        cartesian.yAxis(0).title("Population");
        cartesian.xAxis(0).title("Tick");
        cartesian.crosshair().enabled(true);
        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);
        cartesian.legend().enabled(true);

        applyDarkChartStyle(cartesian);

        Set set = Set.instantiate();
        set.data(dataEntries);

        for (int i = 0; i < speciesToGraph.size(); i++) {
            String mappingKey = i == 0 ? "value" : "value" + i;
            Mapping mapping = set.mapAs(
                    String.format(Locale.US, "{ x: 'x', value: '%s' }", mappingKey)
            );

            Line line = cartesian.line(mapping);
            line.name(speciesToGraph.get(i));
            line.hovered().markers().enabled(true);
            line.hovered().markers().type(MarkerType.CIRCLE).size(4d);
            line.tooltip()
                    .position("right")
                    .anchor(Anchor.LEFT_CENTER)
                    .offsetX(5d)
                    .offsetY(5d);
        }

        anyChartView.setChart(cartesian);
        anyChartView.setVisibility(View.VISIBLE);
        anyChartView.invalidate();
        anyChartView.requestLayout();
    }

    private void recreateChartView() {
        removeChartView();

        anyChartView = new AnyChartView(requireContext());
        anyChartView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        anyChartView.setBackgroundColor(Color.TRANSPARENT);

        int insertIndex = chartPlaceholderText == null ? chartContainer.getChildCount() : 1;
        chartContainer.addView(anyChartView, insertIndex);
    }

    private void removeChartView() {
        if (chartContainer != null && anyChartView != null) {
            chartContainer.removeView(anyChartView);
        }
        anyChartView = null;
    }

    private void applyDarkChartStyle(@NonNull Cartesian cartesian) {
        String surface = getColorHex(R.color.bg_surface);
        String textPrimary = getColorHex(R.color.text_primary);
        String textMuted = getColorHex(R.color.text_muted);

        cartesian.background().fill(surface);

        cartesian.title().fontColor(textPrimary);

        cartesian.xAxis(0).labels().fontColor(textMuted);
        cartesian.yAxis(0).labels().fontColor(textMuted);

        cartesian.xAxis(0).title().fontColor(textPrimary);
        cartesian.yAxis(0).title().fontColor(textPrimary);

        cartesian.legend().fontColor(textPrimary);

        cartesian.xGrid(0).stroke(GRID_MAJOR);
        cartesian.yGrid(0).stroke(GRID_MAJOR);
        cartesian.xMinorGrid(0).stroke(GRID_MINOR);
        cartesian.yMinorGrid(0).stroke(GRID_MINOR);
    }

    private String getColorHex(int colorResId) {
        int color = ContextCompat.getColor(requireContext(), colorResId);
        return String.format("#%06X", (0xFFFFFF & color));
    }

    private void renderSummary() {
        if (summaryText == null) {
            return;
        }

        if (history.isEmpty()) {
            summaryText.setText("Simulation running · 0 ticks");
            return;
        }

        StatisticsSnapshot latest = history.get(history.size() - 1);

        int aliveSpecies = 0;
        int extinctSpecies = 0;
        String dominantSpecies = "None";
        int maxPopulationValue = -1;

        for (String species : knownSpecies) {
            int population = getPopulation(latest, species);

            if (population > 0) {
                aliveSpecies++;
            } else {
                extinctSpecies++;
            }

            if (population > maxPopulationValue) {
                maxPopulationValue = population;
                dominantSpecies = species + " (" + population + ")";
            }
        }

        List<String> speciesToGraph = getSpeciesToGraph();

        String summary =
                "Tick " + latest.getTick() +
                        " · Total population " + latest.getTotalPopulation() +
                        "\nAlive species: " + aliveSpecies +
                        " · Extinct species: " + extinctSpecies +
                        "\nDominant: " + dominantSpecies +
                        " · Status: " + resolveStatus(latest) +
                        "\nGraph: " + describeGraphMode(speciesToGraph);

        summaryText.setText(summary);
    }

    private List<String> getSpeciesToGraph() {
        switch (graphMode) {
            case ALL_SPECIES:
                return new ArrayList<>(knownSpecies);

            case ANIMALS_ONLY:
                return filterKnownSpecies(DEFAULT_ANIMALS);

            case PLANTS_ONLY:
                return filterKnownSpecies(DEFAULT_PLANTS);

            case TOP_CURRENT:
            default:
                return getTopSpeciesByCurrentPopulation(maxSeries);
        }
    }

    private List<String> filterKnownSpecies(@NonNull LinkedHashSet<String> allowedSpecies) {
        List<String> result = new ArrayList<>();
        for (String species : knownSpecies) {
            if (allowedSpecies.contains(species)) {
                result.add(species);
            }
        }
        return result;
    }

    private List<String> getTopSpeciesByCurrentPopulation(int limit) {
        List<String> result = new ArrayList<>();

        if (history.isEmpty()) {
            return result;
        }

        StatisticsSnapshot latest = history.get(history.size() - 1);
        List<Map.Entry<String, Integer>> populations = new ArrayList<>();

        for (String species : knownSpecies) {
            populations.add(new AbstractMap.SimpleEntry<>(
                    species,
                    getPopulation(latest, species)
            ));
        }

        populations.sort((a, b) -> {
            int byPopulation = Integer.compare(b.getValue(), a.getValue());
            if (byPopulation != 0) {
                return byPopulation;
            }
            return a.getKey().compareToIgnoreCase(b.getKey());
        });

        int count = Math.min(Math.max(1, limit), populations.size());
        for (int i = 0; i < count; i++) {
            result.add(populations.get(i).getKey());
        }

        return result;
    }

    private int getPopulation(@NonNull StatisticsSnapshot snapshot, @NonNull String species) {
        Integer value = snapshot.getPopulationBySpecies().get(species);
        return value == null ? 0 : value;
    }

    private String buildChartTitle(@NonNull List<String> speciesToGraph) {
        switch (graphMode) {
            case ALL_SPECIES:
                return "All Species";

            case ANIMALS_ONLY:
                return "Animals Only";

            case PLANTS_ONLY:
                return "Plants Only";

            case TOP_CURRENT:
            default:
                return "Top " + speciesToGraph.size() + " Species";
        }
    }

    private String describeGraphMode(@NonNull List<String> speciesToGraph) {
        switch (graphMode) {
            case ALL_SPECIES:
                return "showing all " + speciesToGraph.size() + " species";

            case ANIMALS_ONLY:
                return "showing animals only (" + speciesToGraph.size() + ")";

            case PLANTS_ONLY:
                return "showing plants only (" + speciesToGraph.size() + ")";

            case TOP_CURRENT:
            default:
                return "showing top " + speciesToGraph.size() + " by current population";
        }
    }

    private String resolveStatus(@NonNull StatisticsSnapshot latest) {
        String explicitStatus = latest.getStatus();
        if (explicitStatus != null && !explicitStatus.trim().isEmpty()) {
            return explicitStatus;
        }

        int totalPopulation = latest.getTotalPopulation();

        if (totalPopulation == 0) {
            return "Extinct";
        }

        if (history.size() < 2) {
            return "Starting";
        }

        int previousPopulation = history.get(history.size() - 2).getTotalPopulation();

        if (previousPopulation == 0 && totalPopulation > 0) {
            return "Recovering";
        }

        int delta = totalPopulation - previousPopulation;
        int threshold = Math.max(2, (int) Math.ceil(previousPopulation * 0.10));

        if (delta >= threshold) {
            return "Growing";
        }

        if (delta <= -threshold) {
            return "Declining";
        }

        return "Stable";
    }

    private static final class ChartDataEntry extends ValueDataEntry {

        ChartDataEntry(int tick, List<String> species, Map<String, Integer> populations) {
            super(String.valueOf(tick), getPopulationForIndex(species, populations, 0));

            for (int i = 1; i < species.size(); i++) {
                setValue("value" + i, getPopulationForIndex(species, populations, i));
            }
        }

        private static int getPopulationForIndex(
                List<String> species,
                Map<String, Integer> populations,
                int index
        ) {
            if (index < 0 || index >= species.size()) {
                return 0;
            }

            Integer value = populations.get(species.get(index));
            return value == null ? 0 : value;
        }
    }
}