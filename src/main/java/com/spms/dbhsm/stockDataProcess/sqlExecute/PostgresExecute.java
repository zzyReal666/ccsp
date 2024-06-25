package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/6/25 13:43
 */
public class PostgresExecute implements SqlExecuteSPI {

    private static final Logger log = LoggerFactory.getLogger(PostgresExecute.class);

    //查询主键
    public static final String GET_PRIMARY_KEY_SQL = "SELECT a.attname as primary_key\n" + "FROM pg_index i\n" + "         JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)\n" + "WHERE i.indrelid = '<tableName>'::regclass\n" + "  AND i.indisprimary";

    //新增列主句
    public static final String ADD_COLUMN = "ALTER TABLE <tableName> ";

    //新增列子句
    private static final String ADD_COLUMN_LOOP = "ADD COLUMN <columnName> <columnType> ,";

    //统计表中数据量语句
    private static final String COUNT_DATA = "SELECT COUNT(*) FROM <table>";

    //分页查询
    private static final String PAGE_QUERY = "SELECT <columns> FROM <table>  ORDER BY <id> LIMIT <limit> OFFSET <offset>";

    //更新语句
    private static final String UPDATE = "UPDATE <table> SET <set> WHERE <where>";

    //重命名列语句
    private static final String RENAME_COLUMN = "ALTER TABLE <table> RENAME COLUMN <old> TO <new>";

    //删除列主句
    private static final String DROP_COLUMN = "ALTER TABLE <table> <drop>";

    //删除列子句
    private static final String DROP_COLUMN_LOOP = "DROP COLUMN <column>";


    @Override
    public String getPrimaryKey(Connection conn, String schema, String table) {
        String sql = new ST(GET_PRIMARY_KEY_SQL).add("tableName", table).render();
        try (Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            return resultSet.getString("primary_key");
        } catch (SQLException e) {
            log.error("get primary key error,sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTempColumnSuffix() {
        return "_temp$zAyK_dbEnc_postgres_";
    }

    @Override
    public void addTempColumn(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList) {
        String sql = new ST(ADD_COLUMN).add("tableName", table).render();
        StringBuilder sb = new StringBuilder(sql);
        for (AddColumnsDTO addColumnsDTO : addColumnsDtoList) {
            sb.append(new ST(ADD_COLUMN_LOOP)
                    .add("columnName", addColumnsDTO.getColumnName() + getTempColumnSuffix())
                    .add("columnType", addColumnsDTO.isEncrypt() ? "text" : addColumnsDTO.getColumnDefinition().get("type"))
                    .render());
        }
        sb.deleteCharAt(sb.length() - 1);
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(sb.toString());
        } catch (SQLException e) {
            log.error("add temp column error,sql:{}", sb, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int count(Connection conn, String table) {
        String sql = new ST(COUNT_DATA).add("table", table).render();
        try (Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            log.error("count data error,sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    @Override

    public List<Map<String, String>> selectColumn(Connection conn, String table, List<String> columns, int limit, int offset) {
        String sql = new ST(PAGE_QUERY).add("columns", String.join(",", columns)).add("table", table).add("id", columns.get(0)).add("limit", limit).add("offset", offset).render();
        try (Statement statement = conn.createStatement()) {
            List<Map<String, String>> list = new ArrayList<>();
            //结果集
            ResultSet resultSet = statement.executeQuery(sql);
            //metadata
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            //遍历结果集，查询出的数据放在map中，map放在list中，
            while (resultSet.next()) {
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    map.put(metaData.getColumnName(i), resultSet.getString(i));
                }
                list.add(map);
            }
            return list;
        } catch (SQLException e) {
            log.error("select column error,sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void batchUpdate(Connection conn, String table, List<Map<String, String>> data) {
        //开启批量
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            log.error("setAutoCommit to false error", e);
            throw new RuntimeException(e);
        }

        try (Statement statement = conn.createStatement()) {
            //批量更新语句
            StringBuilder set = new StringBuilder();
            StringBuilder where = new StringBuilder();
            data.forEach(map -> {
                AtomicBoolean isFirst = new AtomicBoolean(true);
                map.forEach((k, v) -> {
                    //第一个是主键
                    if (isFirst.getAndSet(false)) {
                        where.append(k).append(" = ").append(v);
                    } else {
                        set.append(k).append(getTempColumnSuffix()).append(" = ").append("'").append(v).append("'").append(",");
                    }
                });
                //删除最后一个逗号
                set.deleteCharAt(set.length() - 1);
                String sql = new ST(UPDATE).add("table", table).add("set", set).add("where", where).render();
                set.setLength(0);
                where.setLength(0);
                try {
                    statement.addBatch(sql);
                } catch (SQLException e) {
                    log.error("add batch error,sql:{}", sql, e);
                    throw new RuntimeException(e);
                }
            });
            statement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                log.error("Rollback failed", rollbackEx);
            }
            log.error("Batch update error", e);
            throw new RuntimeException(e);
        } finally {
            // 恢复自动提交模式
            try {
                conn.setAutoCommit(true);
            } catch (SQLException autoCommitEx) {
                log.error("Reset auto commit failed", autoCommitEx);
            }
        }


    }

    @Override
    public void renameColumn(Connection conn, String schema, String table, String oldColumn, String newColumn) {
        String sql = new ST(RENAME_COLUMN).add("table", table).add("old", oldColumn).add("new", newColumn).render();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("renameColumn {}->{} error", oldColumn, newColumn, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dropColumn(Connection conn, String table, List<String> columns) {
        StringBuilder drop = new StringBuilder();
        columns.forEach(col -> {
            String dropLoop = new ST(DROP_COLUMN_LOOP).add("column", col).render();
            drop.append(dropLoop).append(",");
        });
        drop.deleteCharAt(drop.length() - 1);
        String sql = new ST(DROP_COLUMN).add("table", table).add("drop", drop).render();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("dropColumn {} error", columns, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * postgres连接时 设置search path
     *
     * @param conn        连接
     * @param databaseDTO 数据库信息 table中包含schema
     */
    @Override
    public void connectionOperate(Connection conn, DatabaseDTO databaseDTO) {
        try (Statement statement = conn.createStatement()) {
            statement.execute("set search_path to '" + databaseDTO.getTableDTOList().get(0).getSchema() + "'");
        } catch (SQLException e) {
            log.error("connectionOperate error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getType() {
        return DatabaseTypeEnum.PostgresSQL.name();
    }
}
