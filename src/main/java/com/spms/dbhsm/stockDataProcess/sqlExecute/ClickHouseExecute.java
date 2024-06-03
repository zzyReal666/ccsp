package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;
import lombok.extern.slf4j.Slf4j;
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

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/5/28 14:57
 */
@Slf4j
public class ClickHouseExecute implements SqlExecuteSPI {

    //创建临时表
    private static final String CREATE_TEMP_TABLE = "CREATE TABLE <table><suffix> AS <table>";

    //更改字段类型-主句
    private static final String ALTER_TABLE_COLUMN = "ALTER TABLE <table><suffix> ";

    //更改字段类型-循环段
    private static final String ALTER_TABLE_COLUMN_LOOP = "MODIFY COLUMN <column> <type>";

    //查询全部字段
    private static final String SELECT_ALL_COLUMN_NAMES = "SELECT name FROM system.columns WHERE database = '<databaseName>' AND table = '<tableName>'";

    //新增代替更新 ,需要更新的字段做替换
    private static final String INSERT = "INSERT INTO <table><suffix> (<insertColumns>)  SELECT <selectColumns> FROM <table> ORDER BY <id> LIMIT <limit> OFFSET <offset>";

    //修改表名
    private static final String RENAME_TABLE = "RENAME TABLE <old> TO <new>";

    //获取主键语句模版 - 对于clickhouse是获取的排序键
    private static final String GET_PRIMARY_KEY_SQL = "SELECT name FROM system.columns WHERE database = '<databaseName>' AND table = '<tableName>' AND is_in_primary_key = 1";

    //后缀/前缀
    private static final String TEMP_COLUMN_SUFFIX = "_temp_zAyK_dbEnc_ClickHouse_";

    //统计表中数据量语句
    private static final String COUNT_DATA = "SELECT COUNT(*) FROM <table>";

    //分页查询
    private static final String SELECT_COLUMN = "SELECT <columns> FROM <table>  ORDER BY <id> LIMIT <limit> OFFSET <offset>";

    //删除表
    private static final String DROP = "DROP TABLE <table>";


    //查询Clickhouse的  排序键
    @Override
    public String getPrimaryKey(Connection conn, String schema, String table) throws SQLException {
        String sql = new ST(GET_PRIMARY_KEY_SQL).add("databaseName", schema).add("tableName", table).render();

        try (Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                return resultSet.getString("name");
            }
            return null;
        } catch (Exception e) {
            log.error("getPrimaryKey error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTempColumnSuffix() {
        return TEMP_COLUMN_SUFFIX;
    }

    @Override
    public void addTempColumn(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList) {
        //clickhouse 采用 新建表替代行式的新建列行为，并且将全部数据导入新表，需要加密的做处理
        //新建临时表
        createTempTable(conn, table);
        //修改数据类型
        changeDataType(conn, table, addColumnsDtoList);
    }

    private static void createTempTable(Connection conn, String table) {
        String sql = new ST(CREATE_TEMP_TABLE).add("table", table).add("suffix", TEMP_COLUMN_SUFFIX).render();
        log.info("createTempTable sql: {}", sql);
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            log.error("createTempTable error", e);
            throw new RuntimeException(e);
        }
    }

    private static void changeDataType(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList) {
        StringBuilder sql = new StringBuilder(new ST(ALTER_TABLE_COLUMN).add("table", table).add("suffix", TEMP_COLUMN_SUFFIX).render());
        addColumnsDtoList.forEach(addColumnsDto -> {
            String loop = new ST(ALTER_TABLE_COLUMN_LOOP).add("column", addColumnsDto.getColumnName()).add("type", "String").render();
            sql.append(loop).append(",");
        });
        //删除最后一个逗号
        sql.deleteCharAt(sql.length() - 1);
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql.toString());
        } catch (Exception e) {
            log.error("addTempTable error", e);
        }
    }

    @Override
    public int count(Connection conn, String table) {
        String sql = new ST(COUNT_DATA).add("table", table).render();
        try (Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            return resultSet.getInt(1);
        } catch (Exception e) {
            log.error("count error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 分页查询指定的字段，可以指定多个字段，必须查询主键字段，主键字段放在第一个
     */
    @Override
    public List<Map<String, String>> selectColumn(Connection conn, String table, List<String> columns, int limit, int offset) {
        String id = columns.remove(0);
        String columnStr = String.join(",", columns);
        String sql = new ST(SELECT_COLUMN).add("columns", columnStr).add("id", id).add("table", table).add("limit", limit).add("offset", offset).render();
        log.info("selectColumn sql:{}", sql);
        try (Statement statement = conn.createStatement()) {
            List<Map<String, String>> maps = new ArrayList<>();
            //结果集
            ResultSet resultSet = statement.executeQuery(sql);
            //metadata
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            //遍历结果集，查询出的数据放在map中，map放在list中，
            while (resultSet.next()) {
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String columnValue = resultSet.getString(i);
                    map.put(columnName, columnValue);
                }
                maps.add(map);
            }
            return maps;
        } catch (SQLException e) {
            log.error("selectColumn error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void batchUpdate(Connection conn, String table, List<Map<String, String>> data, int limit, int offset) {
    }

    @Override
    public void columnBatchUpdate(Object... args) throws SQLException {
        //获取参数
        Connection conn = (Connection) args[0];
        String table = (String) args[1];
        String id = (String) args[2];
        Map<String, List<Map<String, String>>> data = (Map<String, List<Map<String, String>>>) args[3];
        int limit = (int) args[4];
        int offset = (int) args[5];
        //全部字段名字
        List<String> allColumnNames = getAllColumnNames(conn, table);
        //拼接sql
        String sql = getInsertSql(allColumnNames, data, table, id, limit, offset);
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            log.error("columnBatchUpdate error", e);
            throw new RuntimeException(e);
        }
    }

    private static String getInsertSql(List<String> allColumnNames, Map<String, List<Map<String, String>>> data, String table, String id, int limit, int offset) {
        StringBuilder insertColumns = new StringBuilder();
        StringBuilder selectColumns = new StringBuilder();
        allColumnNames.forEach(col -> {
            insertColumns.append(col).append(",");
            if (data.containsKey(col)) {
                selectColumns.append("CASE ");
                data.get(col).forEach(plain_cipher_map -> {
                    selectColumns.append("WHEN ").append(col).append(" = '").append(plain_cipher_map.get("plain")).append("' THEN '").append(plain_cipher_map.get("cipher")).append("' ");
                });
                selectColumns.append("END AS ").append(col).append(",");
            } else {
                selectColumns.append(col).append(",");
            }
        });
        selectColumns.deleteCharAt(selectColumns.length() - 1);
        insertColumns.deleteCharAt(insertColumns.length() - 1);

        String sql = new ST(INSERT)
                .add("table", table)
                .add("suffix", TEMP_COLUMN_SUFFIX)
                .add("insertColumns", insertColumns)
                .add("id", id)
                .add("selectColumns", selectColumns)
                .add("limit", limit)
                .add("offset", offset).render();
        log.info("columnBatchUpdate sql:{}", sql);
        return sql;
    }

    private static List<String> getAllColumnNames(Connection conn, String table) throws SQLException {
        //数据库实例
        String currentDatabase = conn.getCatalog();
        //查询全部字段
        String sql = new ST(SELECT_ALL_COLUMN_NAMES).add("databaseName", currentDatabase).add("tableName", table).render();
        try (Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            List<String> columnList = new ArrayList<>();
            while (resultSet.next()) {
                columnList.add(resultSet.getString("name"));
            }
            return columnList;
        } catch (Exception e) {
            log.error("getAllColumnNames error", e);
            throw new RuntimeException(e);
        }
    }

    //对于 clickHouse 是切换两个表的名字
    @Override
    public void renameColumn(Connection conn, String schema, String table, String oldColumn, String newColumn) {
        //旧表切换为 前缀+表名
        oldRenameToNew(conn, table);
        //临时表 表名+后缀 切换为 原表名
        newRenameToOld(conn, table);
    }

    private void newRenameToOld(Connection conn, String table) {
        String sql = new ST(RENAME_TABLE).add("old", table + TEMP_COLUMN_SUFFIX).add("new", table).render();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            log.error("renameColumn error sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    private static void oldRenameToNew(Connection conn, String table) {
        String sql = new ST(RENAME_TABLE).add("old", table).add("new", TEMP_COLUMN_SUFFIX + table).render();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            log.error("renameColumn error sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    //对于ClickHouse 是删除原来的表
    @Override
    public void dropColumn(Connection conn, String table, List<String> columns) {
        //删除旧表
        String sql = new ST(DROP).add("table", table).render();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            log.error("dropColumn error sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getType() {
        return DatabaseTypeEnum.ClickHouse.name();
    }

}
