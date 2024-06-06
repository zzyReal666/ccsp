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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/6/6 11:08
 */
@Slf4j
public class KingBaseExecute implements SqlExecuteSPI {

    //获取主键
    private static final String GET_PRIMARY_KEY_SQL = "SELECT a.attname AS pk\n" + "FROM pg_constraint AS c\n" + "         JOIN pg_attribute AS a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)\n" + "         JOIN pg_class AS t ON t.oid = c.conrelid\n" + "WHERE c.contype = 'p'\n" + "  AND t.relname = '<tableName>'";

    //临时字段后缀
    private static final String TEMP_COLUMN_SUFFIX = "_temp$zAyK_dbEnc_KingBase_";

    //添加字段主句
    private static final String ADD_COLUMN = "ALTER TABLE <table> ";

    //添加字段字句 循环段
    private static final String ADD_COLUMN_LOOP = "ADD <field> <type> <null> <default>";

    //统计表中数据量语句
    private static final String COUNT_DATA = "SELECT COUNT(*) FROM <table>";

    //分页查询
    private static final String SELECT_COLUMN = "SELECT <columns> FROM <table>  ORDER BY <id> LIMIT <limit> OFFSET <offset>";

    //更新语句
    private static final String UPDATE = "UPDATE <table> SET <set> WHERE <where>";

    //重命名字段
    private static final String RENAME_COLUMN = "ALTER TABLE <table> RENAME COLUMN <old> TO <new>";

    //删除字段
    private static final String DROP_COLUMN = "ALTER TABLE <table> <drop>";

    //删除字段循环段
    private static final String DROP_COLUMN_LOOP = "DROP COLUMN <column>";


    @Override
    public String getPrimaryKey(Connection conn, String schema, String table) throws SQLException {
        String sql = new ST(GET_PRIMARY_KEY_SQL).add("tableName", table).render();
        log.info("getPrimaryKey sql:{}", sql);
        try (Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            return resultSet.getString(1);
        } catch (SQLException e) {
            log.error("getPrimaryKey error sql:{}", sql, e);
            throw e;
        }
    }

    @Override
    public String getTempColumnSuffix() {
        return TEMP_COLUMN_SUFFIX;
    }

    @Override
    public void addTempColumn(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList) {
        StringBuilder sql = new StringBuilder().append(new ST(ADD_COLUMN).add("table", table).render());
        addColumnsDtoList.forEach(addColumnsDTO -> {
            Map<String, String> columnDefinition = addColumnsDTO.getColumnDefinition();
            //加密 新增临时字段，固定text类型
            if (addColumnsDTO.isEncrypt()) {
                String definitionSql = new ST(ADD_COLUMN_LOOP).add("field", addColumnsDTO.getColumnName() + TEMP_COLUMN_SUFFIX).add("type", "text").add("null", addColumnsDTO.isNotNull() ? "NOT NULL" : "").add("default", "").render();
                sql.append(definitionSql).append(",");
            }
            //解密 还原为原始字段
            else {
                String definitionSql = new ST(ADD_COLUMN_LOOP).add("field", addColumnsDTO.getColumnName() + TEMP_COLUMN_SUFFIX).add("type", columnDefinition.get("type")).add("null", "NO".equals(columnDefinition.get("null")) ? "NOT NULL" : "").render();
                sql.append(definitionSql).append(",");
            }
        });
        //删除最后一个逗号
        sql.deleteCharAt(sql.length() - 1);
        log.info("addTempColumn sql:{}", sql);
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql.toString());
        } catch (SQLException e) {
            log.error("addTempColumn error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int count(Connection conn, String table) {
        String sql = new ST(COUNT_DATA).add("table", table).render();
        log.info("count sql:{}", sql);
        try (Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            log.error("count error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Map<String, String>> selectColumn(Connection conn, String table, List<String> columns, int limit, int offset) {
        String columnStr = String.join(",", columns);
        String sql = new ST(SELECT_COLUMN).add("columns", columnStr).add("id", columns.get(0)).add("table", table).add("limit", limit).add("offset", offset).render();
        log.info("selectColumn sql:{}", sql);
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
                    String columnName = metaData.getColumnName(i);
                    String columnValue = resultSet.getString(i);
                    map.put(columnName, columnValue);
                }
                list.add(map);
            }
            return list;
        } catch (SQLException e) {
            log.error("selectColumn error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void batchUpdate(Connection conn, String table, List<Map<String, String>> data) {
        StringBuilder set = new StringBuilder();
        StringBuilder where = new StringBuilder();
        //开启批量
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            log.error("setAutoCommit to false error", e);
            throw new RuntimeException(e);
        }
        try (Statement statement = conn.createStatement()) {
            data.forEach(map -> {
                AtomicBoolean isFirst = new AtomicBoolean(true);
                map.forEach((k, v) -> {
                    //第一个是主键
                    if (isFirst.get()) {
                        where.append(k).append(" = ").append(v);
                    } else {
                        set.append(k).append(TEMP_COLUMN_SUFFIX).append(" = ").append("'").append(v).append("'").append(",");
                    }
                    isFirst.set(false);
                });
                //删除最后一个逗号
                set.deleteCharAt(set.length() - 1);
                String sql = new ST(UPDATE).add("table", table).add("set", set).add("where", where).render();
                set.setLength(0);
                where.setLength(0);
                log.info("batchUpdate sql {}", sql);
                try {
                    statement.addBatch(sql);
                } catch (SQLException e) {
                    log.error("addBatch error", e);
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
        log.info("renameColumn sql:{}", sql);
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("renameColumn error", e);
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
        log.info("dropColumn sql:{}", sql);
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("dropColumn {} error", columns, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getType() {
        return DatabaseTypeEnum.KingBase.name();
    }
}
