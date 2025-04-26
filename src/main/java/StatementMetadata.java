import java.util.ArrayList;
import java.util.List;

/**
 * Holds metadata extracted from the PhonePe statement PDF.
 */
public class StatementMetadata {

    private final String ownerName;
    private final String dateRange; // Stores the full second line containing the date range

    private final String[] columns; // Stores column
    List<TransactionRecord> transactions;
    /**
     * Constructor for StatementMetadata.
     * @param ownerName The name extracted from the first line.
     * @param dateRange The date range string extracted from the second line.
     */
    public StatementMetadata(String ownerName, String dateRange, String[] columns) {
        // Use trim() to remove potential leading/trailing whitespace
        this.ownerName = ownerName != null ? ownerName.trim() : null;
        this.dateRange = dateRange != null ? dateRange.trim() : null;
        this.columns = columns;
        transactions = new ArrayList<>();
    }

    // Getter methods
    public String getOwnerName() {
        return ownerName;
    }

    public String getDateRange() {
        return dateRange;
    }

    @Override
    public String toString() {
        return "StatementMetadata{" +
               "ownerName='" + ownerName + '\'' +
               ", dateRange='" + dateRange + '\'' +
               ", transactions count='" + transactions.size() + '\'' +
               '}';
    }

    public List<TransactionRecord> getTransactions() {
        // Return an unmodifiable list view or a copy if modification outside is a concern
        return transactions; // Or return Collections.unmodifiableList(transactions);
    }

    // Setter for transactions (used by parseReport)
    public void setTransactions(List<TransactionRecord> transactions) {
        this.transactions = (transactions != null) ? transactions : new ArrayList<>();
    }
}