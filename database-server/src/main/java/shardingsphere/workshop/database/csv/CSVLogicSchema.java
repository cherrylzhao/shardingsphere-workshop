
package shardingsphere.workshop.database.csv;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.JDBCType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Logic schema.
 */
@Getter
public class CSVLogicSchema {
    
    private final static CSVLogicSchema instance = new CSVLogicSchema();
    
    private final String schemaName = "sharding-db";
    
    private final Map<String, Collection<ColumnMetaData>> metadata = new HashMap<>();
    
    private final Map<String, File> repository = new HashMap<>();
    
    /**
     * Get instance.
     *
     * @return CSV logic schema
     */
    public static CSVLogicSchema getInstance() {
        return instance;
    }
    
    /**
     * init.
     *
     * @throws IOException IO exception
     */
    public void init() throws IOException {
        File csvDirectory = new File(CSVLogicSchema.class.getResource("/data").getFile());
        File[] files = csvDirectory.listFiles((file) -> file.getName().endsWith(".csv"));
        if (null == files) {
            return;
        }
        for (File each : files) {
            String tableName = each.getName().replace(".csv", "");
            try (CSVReader csvReader = getCSVReader(each)) {
                String[] header = csvReader.readNext();
                loadMetaData(tableName, header);
            }
            repository.put(tableName, each);
        }
    }
    
    private CSVReader getCSVReader(final File file) throws FileNotFoundException {
        return new CSVReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }
    
    private void loadMetaData(final String tableName, final String[] header) {
        Collection<ColumnMetaData> columnMetaDataList = new LinkedList<>();
        int columnIndex = 1;
        for (String each : header) {
            String[] column = each.split(":");
            Preconditions.checkState(column.length == 2, "csv file header should be column:type pattern.");
            columnMetaDataList.add(new ColumnMetaData(column[0], CSVFieldType.of(column[1]).jdbcType.getVendorTypeNumber(), columnIndex++));
        }
        metadata.put(tableName, columnMetaDataList);
    }
    
    /**
     * Get CSV reader.
     *
     * @param name file name
     * @return CSV reader
     * @throws FileNotFoundException File not found exception
     */
    public CSVReader getCSVReader(final String name) throws FileNotFoundException {
        return new CSVReader(new InputStreamReader(new FileInputStream(repository.get(name)), StandardCharsets.UTF_8));
    }
    
    @RequiredArgsConstructor
    @Getter
    public final class ColumnMetaData {
        
        private final String name;
        
        private final int dataType;
        
        private final int columnIndex;
    }
    
    @RequiredArgsConstructor
    public enum CSVFieldType {
        
        LONG(JDBCType.BIGINT, "long"),
        
        INT(JDBCType.INTEGER, "int"),
        
        STRING(JDBCType.VARCHAR, "string");
    
        private final JDBCType jdbcType;
        
        private final String simpleName;
        
        public static CSVFieldType of(final String simpleName) {
            return Arrays.stream(CSVFieldType.values()).filter(each -> simpleName.endsWith(each.simpleName)).findFirst().orElse(null);
        }
    }
}
