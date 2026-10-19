package com.rehab.admin.service.backup;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 纯 JDBC 逻辑备份/恢复引擎（不依赖 mysqldump 二进制，跨平台可运行）。
 *
 * <p>备份文件为自描述 SQL 文本，每条语句前以 {@link #STMT_MARK} 标记行分隔，
 * 恢复时按标记切分逐条执行——即使正文数据中含有分号也不会误切。</p>
 *
 * <p>覆盖范围：当前库全部业务表；Flyway 版本表 {@code flyway_schema_history} 排除
 * （恢复后迁移基线保持不动，避免校验冲突）。</p>
 */
@Slf4j
@Component
public class SqlBackupEngine {

    /** 语句分隔标记（独占一行，恢复按此切分） */
    public static final String STMT_MARK = "-- >>> STATEMENT <<<";

    /** 每批 INSERT 的行数（多值 INSERT） */
    private static final int INSERT_BATCH_SIZE = 200;

    /** Flyway 版本表不纳入备份/恢复 */
    private static final String FLYWAY_TABLE = "flyway_schema_history";

    private final DataSource dataSource;

    public SqlBackupEngine(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 备份产物元数据 */
    @Getter
    public static class BackupResult {
        private long tableCount;
        private long rowCount;
        private long fileSize;
        private String sha256;
    }

    /**
     * 执行全库逻辑备份。
     *
     * @param targetFile 目标 .sql 文件路径（父目录自动创建）
     */
    public BackupResult backup(Path targetFile, String headerComment) throws SQLException, IOException {
        Files.createDirectories(targetFile.getParent());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 摘要组件不可用", e);
        }

        BackupResult result = new BackupResult();
        try (Connection conn = dataSource.getConnection();
             BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
            writeLine(writer, "-- Rehab Platform logical backup (JDBC engine, mysqldump-free)");
            writeLine(writer, "-- generated_at=" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (headerComment != null && !headerComment.isBlank()) {
                writeLine(writer, "-- " + headerComment.replace("\r", " ").replace("\n", " "));
            }
            writeStatement(writer, "SET FOREIGN_KEY_CHECKS=0");

            List<String> tables = listTables(conn);
            for (String table : tables) {
                result.tableCount++;
                result.rowCount += backupTable(conn, writer, table);
            }

            writeStatement(writer, "SET FOREIGN_KEY_CHECKS=1");
            writer.flush();
        }
        byte[] hash = digest.digest(Files.readAllBytes(targetFile));
        result.sha256 = HexFormat.of().formatHex(hash);
        result.fileSize = Files.size(targetFile);
        log.info("逻辑备份完成：{}，表 {} 张，行 {}，大小 {} 字节",
                targetFile, result.tableCount, result.rowCount, result.fileSize);
        return result;
    }

    /** 备份单张表：DROP + CREATE + 分批 INSERT，返回导出行数 */
    private long backupTable(Connection conn, BufferedWriter writer, String table) throws SQLException, IOException {
        // 1) DDL（SHOW CREATE TABLE 的输出内部换行不含分号，配合标记行安全切分）
        String createSql;
        try (Statement ddlStmt = conn.createStatement();
             ResultSet rs = ddlStmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            rs.next();
            createSql = rs.getString(2);
        }
        writeStatement(writer, "DROP TABLE IF EXISTS `" + table + "`");
        writeStatement(writer, createSql.trim());

        // 2) 数据：MySQL 驱动流式 ResultSet，避免大表全量载入内存
        long rows = 0;
        try (Statement dataStmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            dataStmt.setFetchSize(Integer.MIN_VALUE);
            try (ResultSet rs = dataStmt.executeQuery("SELECT * FROM `" + table + "`")) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                StringBuilder columns = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        columns.append(',');
                    }
                    columns.append('`').append(meta.getColumnName(i)).append('`');
                }

                List<String> valueBatch = new ArrayList<>(INSERT_BATCH_SIZE);
                while (rs.next()) {
                    StringBuilder values = new StringBuilder("(");
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) {
                            values.append(',');
                        }
                        values.append(formatValue(rs, meta, i));
                    }
                    values.append(')');
                    valueBatch.add(values.toString());
                    rows++;
                    if (valueBatch.size() >= INSERT_BATCH_SIZE) {
                        flushInsertBatch(writer, table, columns, valueBatch);
                    }
                }
                if (!valueBatch.isEmpty()) {
                    flushInsertBatch(writer, table, columns, valueBatch);
                }
            }
        }
        return rows;
    }

    private void flushInsertBatch(BufferedWriter writer, String table,
                                  StringBuilder columns, List<String> valueBatch) throws IOException {
        StringBuilder sql = new StringBuilder("INSERT INTO `")
                .append(table).append("` (").append(columns).append(") VALUES ");
        for (int i = 0; i < valueBatch.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(valueBatch.get(i));
        }
        writeStatement(writer, sql.toString());
        valueBatch.clear();
    }

    /**
     * 按备份文件标记逐语句恢复。调用方负责在恢复前后完成快照与审计。
     */
    public void restore(Path sqlFile) throws SQLException, IOException {
        String content = Files.readString(sqlFile, StandardCharsets.UTF_8);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            StringBuilder current = new StringBuilder();
            boolean inside = false;
            for (String rawLine : content.split("\n", -1)) {
                if (STMT_MARK.equals(rawLine.trim())) {
                    if (inside && current.length() > 0) {
                        stmt.execute(current.toString());
                    }
                    current.setLength(0);
                    inside = true;
                    continue;
                }
                if (inside) {
                    if (current.length() > 0) {
                        current.append('\n');
                    }
                    current.append(rawLine);
                }
            }
            if (inside && current.length() > 0) {
                stmt.execute(current.toString());
            }
        }
        log.info("逻辑恢复完成：{}", sqlFile);
    }

    /** 列出当前库全部业务表（排除 Flyway 历史表） */
    private List<String> listTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                String table = rs.getString(1);
                if (!FLYWAY_TABLE.equalsIgnoreCase(table)) {
                    tables.add(table);
                }
            }
        }
        return tables;
    }

    /** 按 JDBC 类型将列值格式化为 MySQL 字面量 */
    private String formatValue(ResultSet rs, ResultSetMetaData meta, int index) throws SQLException {
        Object value = rs.getObject(index);
        if (value == null || rs.wasNull()) {
            return "NULL";
        }
        int sqlType = meta.getColumnType(index);
        if (value instanceof byte[] bytes) {
            return "X'" + HexFormat.of().formatHex(bytes) + "'";
        }
        if (isNumericType(sqlType)
                && (value instanceof Number
                || value instanceof Boolean
                || value instanceof BigDecimal
                || value instanceof BigInteger)) {
            // Boolean 在 MySQL 驱动中映射 TINYINT(1)，按 0/1 裸写
            if (value instanceof Boolean b) {
                return b ? "1" : "0";
            }
            return value.toString();
        }
        return "'" + escapeString(value.toString()) + "'";
    }

    private boolean isNumericType(int sqlType) {
        return sqlType == Types.TINYINT || sqlType == Types.SMALLINT || sqlType == Types.INTEGER
                || sqlType == Types.BIGINT || sqlType == Types.DECIMAL || sqlType == Types.NUMERIC
                || sqlType == Types.REAL || sqlType == Types.FLOAT || sqlType == Types.DOUBLE
                || sqlType == Types.BIT;
    }

    /** MySQL 字符串字面量转义 */
    private String escapeString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\0' -> sb.append("\\0");
                case '\b' -> sb.append("\\b");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case 0x1A -> sb.append("\\Z");
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '"' -> sb.append("\\\"");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void writeStatement(BufferedWriter writer, String sql) throws IOException {
        writer.write(STMT_MARK);
        writer.newLine();
        writer.write(sql);
        writer.newLine();
    }

    private void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    /** 供测试/预检使用：校验备份文件语句切分是否完整（不执行） */
    public int countStatements(Path sqlFile) throws IOException {
        String content = Files.readString(sqlFile, StandardCharsets.UTF_8);
        int count = 0;
        for (String line : content.split("\n", -1)) {
            if (STMT_MARK.equals(line.trim())) {
                count++;
            }
        }
        return count;
    }
}
