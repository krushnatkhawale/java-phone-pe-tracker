
import java.util.Collections;
import java.util.Map;

/**
 * Represents a single transaction record (one row) from the statement.
 * Stores data as a map where the key is the column name and the value is the cell data.
 */
public class TransactionRecord {

    // Use a Map to store column name -> value pairs for flexibility
    private final Map<String, String> data;

    /**
     * Constructor for TransactionRecord.
     * @param data A map containing the column names and their corresponding values for this row.
     */
    public TransactionRecord(Map<String, String> data) {
        // Store an immutable copy of the map for safety
        this.data = (data != null) ? Collections.unmodifiableMap(data) : Collections.emptyMap();
    }

    /**
     * Gets the value for a specific column name.
     * @param columnName The name of the column.
     * @return The value associated with the column, or null if the column doesn't exist for this record.
     */
    public String getValue(String columnName) {
        return data.get(columnName);
    }

    /**
     * Gets the underlying map of all data for this record.
     * Returns an unmodifiable map.
     * @return An unmodifiable map of column names to values.
     */
    public Map<String, String> getData() {
        return data;
    }

    @Override
    public String toString() {
        // Provide a meaningful string representation for debugging
        StringBuilder sb = new StringBuilder("TransactionRecord{");
        data.forEach((key, value) -> sb.append(key).append("='").append(value).append("', "));
        // Remove trailing comma and space if data exists
        if (!data.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append('}');
        return sb.toString();
    }

    // Optional: Add equals() and hashCode() if needed later
}