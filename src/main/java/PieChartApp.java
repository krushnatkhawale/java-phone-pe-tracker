import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class PieChartApp extends Application {
    // --- UI Controls ---
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Button filterButton;
    private PieChart chart; // Make chart a field

    // --- Data ---
    private static StatementMetadata statementData; // Keep original full data
    private List<TransactionRecord> originalTransactions; // Store the full list
    private ObservableList<PieChart.Data> pieChartData; // Data currently shown

    // --- Date Formatting ---
    // IMPORTANT: Adjust the pattern to EXACTLY match the date format in your PDF text
    // Example: "Apr 19, 2024" -> "MMM dd, yyyy"
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);


    // Static method for App.java to set the data before launching
    public static void setStatementData(StatementMetadata data) {
        statementData = data;
    }

    @Override
    public void start(Stage primaryStage) {
        // --- Initial Data Check ---
        if (statementData == null || statementData.getTransactions() == null) {
            showError("No transaction data loaded.");
            primaryStage.setTitle("Spending Chart - Error");
            StackPane root = new StackPane(new Label("Could not load transaction data."));
            primaryStage.setScene(new Scene(root, 400, 300));
            primaryStage.show();
            return;
        }

        // Store the original list
        originalTransactions = new ArrayList<>(statementData.getTransactions()); // Make a copy if needed

        // --- Create UI Controls ---
        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();
        filterButton = new Button("Filter");

        // Set prompts for date pickers
        startDatePicker.setPromptText("Start Date");
        endDatePicker.setPromptText("End Date");

        // --- Create Chart ---
        pieChartData = FXCollections.observableArrayList(); // Initialize chart data list
        chart = new PieChart(pieChartData);
        chart.setTitle("Spending by Category/Recipient");
        chart.setLegendSide(Side.LEFT);
        chart.setLabelsVisible(false);

        // --- Layout ---
        // Control bar at the top
        HBox filterBox = new HBox(10); // Spacing of 10
        filterBox.setPadding(new Insets(10)); // Padding around the box
        filterBox.setAlignment(Pos.CENTER); // Center controls horizontally
        filterBox.getChildren().addAll(
                new Label("From:"), startDatePicker,
                new Label("To:"), endDatePicker,
                filterButton
        );

        // Main layout
        BorderPane root = new BorderPane();
        root.setTop(filterBox);
        root.setCenter(chart);

        // --- Set Button Action ---
        filterButton.setOnAction(event -> applyFilter());

        // --- Initial Chart Population ---
        if (originalTransactions.isEmpty()) {
            System.out.println("No transactions found in the provided data.");
            chart.setTitle("Spending by Category/Recipient (No Transactions)");
        } else {
            // Populate chart initially with all data
            updateChart(originalTransactions);
            // Optionally set default date picker values (e.g., min/max dates from data)
            setInitialDateRange();
        }


        // --- Set up Scene and Stage ---
        Scene scene = new Scene(root, 900, 700); // Increased size
        primaryStage.setTitle("PhonePe Spending Analysis");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Sets the initial date range in the pickers based on the transaction data.
     */
    private void setInitialDateRange() {
        if (originalTransactions == null || originalTransactions.isEmpty()) return;

        LocalDate minDate = null;
        LocalDate maxDate = null;

        for (TransactionRecord record : originalTransactions) {
            LocalDate date = parseDate(record.getValue(App.COL_DATE));
            if (date != null) {
                if (minDate == null || date.isBefore(minDate)) {
                    minDate = date;
                }
                if (maxDate == null || date.isAfter(maxDate)) {
                    maxDate = date;
                }
            }
        }

        startDatePicker.setValue(minDate);
        endDatePicker.setValue(maxDate);
    }


    /**
     * Filters transactions based on selected dates and updates the chart.
     */
    private void applyFilter() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        System.out.println("Filtering from " + startDate + " to " + endDate);

        if (originalTransactions == null) {
            showError("Original transaction data is missing.");
            return;
        }

        // Filter the original list
        List<TransactionRecord> filteredTransactions = originalTransactions.stream()
                .filter(record -> {
                    LocalDate transactionDate = parseDate(record.getValue(App.COL_DATE));
                    if (transactionDate == null) {
                        return false; // Skip records with unparseable dates
                    }

                    // Check against start date (if selected)
                    boolean afterOrOnStartDate = (startDate == null) || !transactionDate.isBefore(startDate);
                    // Check against end date (if selected)
                    boolean beforeOrOnEndDate = (endDate == null) || !transactionDate.isAfter(endDate);

                    return afterOrOnStartDate && beforeOrOnEndDate;
                })
                .collect(Collectors.toList());

        System.out.println("Found " + filteredTransactions.size() + " transactions in the selected range.");

        // Update the chart with the filtered data
        updateChart(filteredTransactions);
    }

    /**
     * Parses a date string using the defined DATE_FORMATTER.
     * @param dateString The date string from the transaction record.
     * @return The parsed LocalDate, or null if parsing fails.
     */
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            // Handle potential multi-line dates if they were concatenated with space
            dateString = dateString.replace("\n", " ").replace("\r", ""); // Clean newlines just in case
            return LocalDate.parse(dateString.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Warning: Could not parse date: '" + dateString + "' - " + e.getMessage());
            return null; // Return null if parsing fails
        }
    }


    /**
     * Updates the PieChart with data aggregated from the given list of transactions.
     * @param transactionsToDisplay The list of transactions to include in the chart.
     */
    private void updateChart(List<TransactionRecord> transactionsToDisplay) {
        // 1. Aggregate Spending Data from the filtered list
        Map<String, Double> spendingByCategory = aggregateSpending(transactionsToDisplay);

        // 2. Prepare Data for Pie Chart
        ObservableList<PieChart.Data> newPieChartData = FXCollections.observableArrayList();
        int limit = 15; // Max categories to show directly
        double totalValue = spendingByCategory.values().stream().mapToDouble(Double::doubleValue).sum(); // Calculate total for percentage (optional)

        spendingByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()) // Sort by amount descending
                .limit(limit)
                .forEach(entry -> {
                    // Format label: "Category: Amount (Percentage%)"
                    double percentage = (totalValue > 0) ? (entry.getValue() * 100.0 / totalValue) : 0.0;
                    String label = String.format("%s: %.2f (%.1f%%)", entry.getKey(), entry.getValue(), percentage);
                    newPieChartData.add(new PieChart.Data(label, entry.getValue()));
                });

        // Handle remaining categories ("Other")
        if (spendingByCategory.size() > limit) {
            double otherAmount = spendingByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .skip(limit)
                    .mapToDouble(Map.Entry::getValue)
                    .sum();
            if (otherAmount > 0) {
                double percentage = (totalValue > 0) ? (otherAmount * 100.0 / totalValue) : 0.0;
                String label = String.format("Other: %.2f (%.1f%%)", otherAmount, percentage);
                newPieChartData.add(new PieChart.Data(label, otherAmount));
            }
        }

        // 3. Update the chart's data
        pieChartData.setAll(newPieChartData); // Efficiently update the list

        // Update title based on whether data is present
        if (transactionsToDisplay.isEmpty()) {
            chart.setTitle("Spending by Category/Recipient (No data in range)");
        } else {
            chart.setTitle("Spending by Category/Recipient");
        }


        // Re-apply tooltips if needed (nodes might be recreated)
        pieChartData.forEach(data -> {
            String tooltipText = String.format("%.2f", data.getPieValue());
            Tooltip tooltip = new Tooltip(tooltipText);
            // Tooltip might need to be installed slightly differently if nodes change significantly,
            // but often works directly on the data object's node property after update.
            if (data.getNode() != null) {
                Tooltip.install(data.getNode(), tooltip);
            } else {
                // If node is null initially, listen for changes (more complex)
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        Tooltip.install(newNode, tooltip);
                    }
                });
            }
        });
    }


    /**
     * Aggregates spending from transactions based on description.
     * (This method remains largely the same, just operates on the provided list)
     * @param transactions List of TransactionRecord objects to aggregate.
     * @return Map where key is description (category) and value is total debit amount.
     */
    private Map<String, Double> aggregateSpending(List<TransactionRecord> transactions) {
        Map<String, Double> spendingMap = new HashMap<>();

        for (TransactionRecord record : transactions) {
            String type = record.getValue(App.COL_TYPE);
            String description = record.getValue(App.COL_DESCRIPTION);
            String amountStr = record.getValue(App.COL_AMOUNT);

            if (type != null && type.equalsIgnoreCase("Debit") && description != null && amountStr != null) {
                try {
                    double amount = Double.parseDouble(amountStr.replace(",", ""));
                    String category = description.trim();
                    if (category.toLowerCase().startsWith("paid to ")) {
                        category = category.substring(8).trim();
                    }
                    // Further category refinement could happen here (e.g., mapping keywords)
                    spendingMap.put(category, spendingMap.getOrDefault(category, 0.0) + amount);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Could not parse amount '" + amountStr + "' for description '" + description + "'. Skipping aggregation.");
                }
            }
        }
        return spendingMap;
    }

    /** Helper to show error messages (optional) */
    private void showError(String message) {
        // Could use javafx.scene.control.Alert for a proper dialog
        System.err.println("Error: " + message);
    }
}