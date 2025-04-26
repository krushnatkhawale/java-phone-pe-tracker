
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Main application class for the PhonePe Tracker.
 */
public class App {

    private static final int OWNER_LINE_INDEX = 0;
    private static final int DATE_RANGE_LINE_INDEX = 1;
    private static final int COLUMNS_LINE_INDEX = 2;
    private static final int DATA_START_LINE_INDEX = 3;
    /**
     * The main entry point of the application.
     * @param args Command line arguments (not used currently).
     */
    public static void main(String[] args) {
        System.out.println("Hello from PhonePe Tracker!");
        // TODO: Add application logic here

        String pdfFilePath = "/Users/hulk/Downloads/pp/PhonePe_k.pdf"; // <--- CHANGE THIS

        File pdfFile = new File(pdfFilePath);

        if (!pdfFile.exists()) {
            System.err.println("Error: PDF file not found at path: " + pdfFilePath);
            return; // Exit if the file doesn't exist
        }

        // Use try-with-resources to ensure the document is closed automatically
        try (PDDocument document = Loader.loadPDF(pdfFile, "7822864892")) {

           // if (!document.isEncrypted()) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                String text = pdfStripper.getText(document);
                System.out.println("\n--- Extracted PDF Text ---");
                System.out.println(text);
                System.out.println("--- End of PDF Text ---");

                parseReport(text);

                // TODO: Add your logic here to process the extracted 'text'
                // For example, search for specific keywords, parse amounts, dates, etc.

          //  } else {
           //     System.err.println("Error: Cannot read encrypted PDF document without a password.");
           // }

        } catch (IOException e) {
            System.err.println("Error reading PDF file: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        }

        // --- End of PDF Reading Logic ---

        System.out.println("\nApplication finished.");
    }

    private static void parseReport(String text) {
        String[] lines = text.split("\\R", 4);
        if (lines.length > 2) {
            String ownerNameLine = lines[0];
            String dateRangeLine = lines[1];
            String columnsLine = lines[2];

            String[] columnNames = extractColumnNames(columnsLine);
            StatementMetadata metadata = new StatementMetadata(ownerNameLine, dateRangeLine, columnNames); // Create the metadata object


            if (columnNames.length > 0) {
                List<TransactionRecord> transactions = extractTransactions(lines[3], columnNames, new ArrayList<>());
                metadata.setTransactions(transactions);
            }

            System.out.println("\n--- Extracted Metadata ---");
            System.out.println(metadata);


            PieChartApp.setStatementData(metadata);
            Application.launch(PieChartApp.class);

            System.out.println("--- End of Metadata ---");

        } else {
            System.err.println("Warning: Could not extract metadata - PDF text has fewer than 2 lines.");
            // Optionally print the first few lines if available for debugging
            if (lines.length > 0) System.err.println("First line: " + lines[0]);
        }
    }



    private static String[] extractColumnNames(String columnsLine) {
        String[] columnNames;
        if (columnsLine != null && !columnsLine.trim().isEmpty()) {
            // Split by one or more whitespace characters
            columnNames = columnsLine.trim().split("\\s+");
        } else {
            // Assign an empty array if the columns line is null or empty/whitespace only
            columnNames = new String[0];
            System.err.println("Warning: Third line (columns) is empty or missing.");
        }
        return columnNames;
    }

    public static final String COL_DATE = "Date";
    public static final String COL_TIME = "Time";
    public static final String COL_DESCRIPTION = "Description";
    public static final String COL_TRANSACTION_ID = "TransactionID";
    public static final String COL_UTR = "UTR";
    public static final String COL_ACCOUNT = "Account";
    public static final String COL_TYPE = "Type"; // Debit or Credit
    public static final String COL_AMOUNT = "Amount";
    public static final String COL_CURRENCY = "Currency";

    private static final int LINES_PER_TRANSACTION = 7;
    private static List<TransactionRecord> extractTransactions(String transactionsInText, String[] columnNames, List<TransactionRecord> transactions) {
        System.out.println("\n--- Parsing Transaction Records ---");
        // Start processing from the line AFTER the column headers
        List<TransactionRecord> parsedTransactions = new ArrayList<>();

        if (transactionsInText == null || transactionsInText.trim().isEmpty()) {
            System.err.println("Warning: Transaction text block is empty.");
            return parsedTransactions; // Return empty list
        }

        transactionsInText = transactionsInText.replaceAll("Page \\d+ of \\d+", "");
        transactionsInText = transactionsInText.replaceAll("Date Transaction Details Type Amount", "");

        String[] lines = transactionsInText.split("\\R"); // Split by any newline sequence

        System.out.println("\n--- Parsing Transaction Records (Multi-line Structure) ---");
        System.out.println("Total lines in transaction block: " + lines.length);
        System.out.println("Expecting " + LINES_PER_TRANSACTION + " lines per transaction.");


        // Iterate through the lines array, taking chunks of LINES_PER_TRANSACTION size
        for (int i = 0; (i + LINES_PER_TRANSACTION) <= lines.length; i += LINES_PER_TRANSACTION) {
            Map<String, String> rowData = new HashMap<>();
            boolean parseSuccess = true; // Flag to track if parsing this chunk worked

            if(lines[i].isBlank() || lines[i].equals("This is a system generated statement. For any queries, contact us at .https://support.phonepe.com/statement")) {
                 i -= (LINES_PER_TRANSACTION-1);
                continue;
            }

            if(lines[i].startsWith("This is an automatically generated statement"))
                break;

            try {
                // Extract data line by line, trimming whitespace
                String date = lines[i].trim();
                if(lines[i+1].charAt(2)!=':'){
                    date += lines[i+1];
                    i++;
                }
                String time = lines[i + 0 + 1].trim();
                String description = lines[i + 0 + 2].trim();
                if(!lines[i+0+3].startsWith("Transaction ID ")){
                    description += " " + lines[i+0+3];
                    i++;
                }
                String txnIdLine = lines[i+ 0 + 3].trim();
                String utrLine = lines[i+ 0 + 4].trim();
                if(!utrLine.startsWith("UTR No")) {
                    utrLine = "Wallets txn has no UTR";
                    i--;
                }
                String accountLine = lines[i+ 0 + 5].trim();
                String amountLine = lines[i+ 0 + 6].trim();
                if(lines[i+7].contains(".")){
                    amountLine = amountLine + " "+ lines[i+7];
                    i = i + 1 + 0;
                }
                // --- Parse specific fields from lines ---

                // Transaction ID (Handles potential variations in spacing)
                String txnId = extractTransactionId(txnIdLine); // Helper method below

                // UTR (Handles potential variations in spacing)
                String utr = utrLine.startsWith("UTR No")? extractUTRNo(utrLine): utrLine; // Helper method below

                // Account (Handles potential variations in spacing)
                String account = accountLine.split("\\s+", 3)[2]; // Helper method below

                // Amount Line (e.g., "Debit INR 550.00" or "Credit 100.00")
                String[] amountParts = amountLine.split("\\s+");
                String type = "";
                String amount = "";
                String currency = ""; // Optional

                if (amountParts.length == 3) {
                    type = amountParts[0]; // "Debit" or "Credit"
                    currency = amountParts[1];
                    amount = amountParts[2]; // Assume amount is last part
                } else {
                    System.err.println("Warning: Could not parse amount line format: '" + amountLine + "' at index " + (i + 6));
                    type = "Unknown";
                    amount = amountLine; // Store the whole line if parsing fails
                    currency = "Unknown";
                    parseSuccess = false;
                }

                // --- Populate the map ---
                rowData.put(COL_DATE, date);
                rowData.put(COL_TIME, time);
                rowData.put(COL_DESCRIPTION, description);
                rowData.put(COL_TRANSACTION_ID, txnId != null ? txnId : txnIdLine); // Fallback to full line
                rowData.put(COL_UTR, utr != null ? utr : utrLine); // Fallback to full line
                rowData.put(COL_ACCOUNT, account != null ? account : accountLine); // Fallback to full line
                rowData.put(COL_TYPE, type);
                rowData.put(COL_CURRENCY, currency);
                rowData.put(COL_AMOUNT, amount);

                // Only add if basic parsing seemed okay (or adjust based on needs)
                // if (parseSuccess) {
                TransactionRecord record = new TransactionRecord(rowData);
                parsedTransactions.add(record);
                // System.out.println("Added Record: " + record); // Uncomment for debugging
                // }

            } catch (ArrayIndexOutOfBoundsException e) {
                // This happens if a transaction chunk is shorter than expected
                System.out.println("Error: Incomplete transaction data found starting near line index " + i + ". Skipping chunk.[[" +
                        lines[i] +
                        "]]");
                // Optionally log the lines that caused the error:
                // String[] errorChunk = Arrays.copyOfRange(lines, i, Math.min(i + LINES_PER_TRANSACTION, lines.length));
                // System.err.println("Problematic lines: " + Arrays.toString(errorChunk));
                // Skip to the next potential transaction start (might be risky if structure varies)
                // Or simply break/stop parsing here depending on desired robustness
            } catch (Exception e) {
                // Catch other potential errors during parsing
                System.out.println("Error parsing transaction chunk starting near line index " + i + ": " + e.getMessage());
                e.printStackTrace(); // Print stack trace for detailed debugging
            }
        }

        // Check for leftover lines
        int leftoverLines = lines.length % LINES_PER_TRANSACTION;
        if (leftoverLines > 0) {
            System.err.println("Warning: Found " + leftoverLines + " leftover line(s) at the end of the transaction block, possibly an incomplete transaction.");
            // Print leftover lines for debugging:
            String[] leftovers = Arrays.copyOfRange(lines, lines.length - leftoverLines, lines.length);
            System.err.println("  Leftover lines: " + Arrays.toString(leftovers));
        }

        System.out.println("Parsed " + parsedTransactions.size() + " transaction records based on multi-line structure.");
        System.out.println("--- End of Transaction Parsing ---");
        return parsedTransactions;

    }

    private static String extractTransactionId(String line) {
        if(line.indexOf(':')>0){
            return extractValueAfterColon(line);
        }
        return line.substring("Transaction ID ".length());
    }
    private static String extractUTRNo(String line) {
        if(line.indexOf(':')>0){
            return extractValueAfterColon(line);
        }
        return line.substring("UTR No. ".length());
    }
    private static String extractValueAfterColon(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex != -1 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        System.err.println("Warning: Could not find ':' or value after it in line: '" + line + "'");
        return null; // Or return the original line, or empty string, depending on desired behavior
    }
}