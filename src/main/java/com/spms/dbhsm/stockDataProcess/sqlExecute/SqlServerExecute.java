package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.stringtemplate.v4.ST;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/6/26 16:02
 */
@Slf4j
public class SqlServerExecute implements SqlExecuteSPI {

    private static final Map<String, String> schemaMap = new ConcurrentHashMap<>();

    //获取主键 PK
    private static final String GET_PRIMARY_KEY_SQL = "SELECT KU.COLUMN_NAME " + "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS TC " + "INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KU " + "ON TC.CONSTRAINT_NAME = KU.CONSTRAINT_NAME " + "AND TC.TABLE_NAME = KU.TABLE_NAME " + "WHERE TC.CONSTRAINT_TYPE = 'PRIMARY KEY' " + "AND KU.TABLE_NAME = ? " + "AND KU.TABLE_SCHEMA = ?";
    //获取自增字段
    private static final String GET_AUTO_INCREMENT_SQL = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? and TABLE_SCHEMA = 'dbo' AND COLUMNPROPERTY(OBJECT_ID(TABLE_NAME), COLUMN_NAME, 'IsIdentity') = 1";

    //添加字段主句
    private static final String ADD_COLUMN = "ALTER TABLE <schema>.<table> ADD ";

    //添加字段字句 循环段
    private static final String ADD_COLUMN_LOOP = " <field> <type> <null> DEFAULT <default>";

    //统计表中数据量语句
    private static final String COUNT_DATA = "SELECT COUNT(*) FROM <schema>.<table>";

    //分页查询
    private static final String SELECT_COLUMN = "WITH PagingCTE AS (\n" + "    SELECT\n" + "        ROW_NUMBER() OVER (ORDER BY <id>) AS RowNum,\n" + "        <columns>\n" + "    FROM\n" + "        <schema>.<table>\n" + ")\n" + "SELECT <columns>\n" + "FROM PagingCTE\n" + "WHERE RowNum BETWEEN <begin> AND <end>";

    //更新语句
    private static final String UPDATE = "UPDATE <schema>.<table> SET <set> WHERE <where>";

    //重命名字段
    private static final String RENAME_COLUMN = "EXEC sp_rename '<schema>.<table>.<old>', '<new>', 'COLUMN';";

    //查询默认值约束
    private static final String GET_DEFAULT_CONSTRAINT_SQL = "SELECT \n" +
            "    dc.name AS ConstraintName,       -- 约束名称\n" +
            "    c.name AS ColumnName,            -- 字段名称\n" +
            "    t.name AS TableName,             -- 表名称\n" +
            "    s.name AS SchemaName,            -- 模式名称\n" +
            "    dc.definition AS DefaultValue    -- 默认值定义\n" +
            "FROM \n" +
            "    sys.default_constraints dc\n" +
            "INNER JOIN \n" +
            "    sys.columns c ON dc.parent_column_id = c.column_id AND dc.parent_object_id = c.object_id\n" +
            "INNER JOIN \n" +
            "    sys.tables t ON dc.parent_object_id = t.object_id\n" +
            "INNER JOIN \n" +
            "    sys.schemas s ON t.schema_id = s.schema_id\n" +
            "WHERE \n" +
            "    s.name = '<schema>'  -- 替换为模式名\n" +
            "    AND t.name = '<table>'  -- 替换为表名\n" +
            "    AND c.name = '<field>';  -- 替换为字段名";

    //删除约束
    private static final String DROP_CONSTRAINT_SQL = "ALTER TABLE <schema>.<table> DROP CONSTRAINT <constraint>";

    //删除字段
    private static final String DROP_COLUMN = "ALTER TABLE <schema>.<table>  DROP COLUMN <drop>";

    //删除字段循环段
    private static final String DROP_COLUMN_LOOP = "<column>";


    @Override
    public String getPrimaryKey(Connection conn, String schema, String table) {
        try (PreparedStatement ps = conn.prepareStatement(GET_PRIMARY_KEY_SQL)) {
            ps.setString(1, table);
            ps.setString(2, schemaMap.get(table));
            ResultSet resultSet = ps.executeQuery();
            resultSet.next();
            log.info("getPrimaryKey success result:{}", resultSet.getString(1));
            return resultSet.getString(1);
        } catch (Exception e) {
            log.warn("getPrimaryKey error sql:{},schema:{},table:{}", GET_PRIMARY_KEY_SQL, schema, table);
            return getAutoIncrement(conn, table);
        }
    }


    private String getAutoIncrement(Connection conn, String table) {
        try (PreparedStatement ps = conn.prepareStatement(GET_AUTO_INCREMENT_SQL)) {
            ps.setString(1, table);
            ResultSet resultSet = ps.executeQuery();
            resultSet.next();
            log.info("getAutoIncrement success result:{}", resultSet.getString(1));
            return resultSet.getString(1);
        } catch (SQLException e) {
            log.error("getAutoIncrement error sql:{},table:{}", GET_PRIMARY_KEY_SQL, table);
            throw new RuntimeException();
        }
    }

    @Override
    public String getTempColumnSuffix() {
        return "Temp";
    }

    @Override
    public void addTempColumn(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList) {
        log.info("addTempColumn start table:{},addColumnsDtoList:{}", table, addColumnsDtoList);
        StringBuilder sql = new StringBuilder().append(new ST(ADD_COLUMN).add("schema", schemaMap.get(table)).add("table", table).render());
        addColumnsDtoList.forEach(addColumnsDTO -> {
            Map<String, String> columnDefinition = addColumnsDTO.getColumnDefinition();
            //加密 新增临时字段，固定nvarchar(MAX)类型
            String nullable = "NO".equals(addColumnsDTO.getColumnDefinition().get("Null")) ? "NOT NULL" :
                    ("YES".equals(addColumnsDTO.getColumnDefinition().get("Null")) ? "NULL" : "");
            String defaultStr = addColumnsDTO.getColumnDefinition().get("Default");
            if ("NOT NULL".equals(nullable) && StringUtils.isBlank(defaultStr)) {
                log.warn("field {} is not null and has no default value, set default value to ''", addColumnsDTO.getColumnName());
                defaultStr = "('')";
            }
                if (addColumnsDTO.isEncrypt()) {
                    String definitionSql = new ST(ADD_COLUMN_LOOP)
                            .add("field", addColumnsDTO.getColumnName() + getTempColumnSuffix())//
                            .add("type", "nvarchar(MAX)")//
                            .add("null", nullable)//
                            .add("default", defaultStr)//
                            .render();
                    sql.append(definitionSql).append(",");
                }
                //解密 还原为原始字段
                else {
                    String definitionSql = new ST(ADD_COLUMN_LOOP)
                            .add("field", addColumnsDTO.getColumnName() + getTempColumnSuffix())//
                            .add("type", columnDefinition.get("type"))//
                            .add("null", nullable)//
                            .add("default", defaultStr)//
                            .render();
                    sql.append(definitionSql).append(",");
                }
            });
        //删除最后一个逗号
        sql.deleteCharAt(sql.length() - 1);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.executeUpdate();
            log.info("addTempColumn success sql:{}", sql);
        } catch (Exception e) {
            log.error("addTempColumn error sql:{}", sql);
            throw new RuntimeException();
        }
    }

    @Override
    public int count(Connection conn, String table) {
        String sql = "";
        try (Statement statement = conn.createStatement()) {
            sql = new ST(COUNT_DATA).add("schema", schemaMap.get(table)).add("table", table).render();
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            log.info("count success result:{}", resultSet.getInt(1));
            return resultSet.getInt(1);
        } catch (SQLException e) {
            log.error("count error sql:{}", sql);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Map<String, String>> selectColumn(Connection conn, String table, List<String> columns, int limit, int offset) {
        String columnStr = String.join(",", columns);
        String sql = new ST(SELECT_COLUMN).add("columns", columnStr).add("id", columns.get(0)).add("schema", schemaMap.get(table)).add("table", table).add("begin", offset).add("end", offset + limit).render();
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
            log.info("selectColumn success");
            return list;
        } catch (SQLException e) {
            log.error("selectColumn error sql:{}", sql);
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
                        set.append(k).append(getTempColumnSuffix()).append(" = ").append("'").append(v).append("'").append(",");
                    }
                    isFirst.set(false);
                });
                //删除最后一个逗号
                set.deleteCharAt(set.length() - 1);
                String sql = new ST(UPDATE).add("schema", schemaMap.get(table)).add("table", table).add("set", set).add("where", where).render();
                set.setLength(0);
                where.setLength(0);
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
        String sql = new ST(RENAME_COLUMN).add("schema", schemaMap.get(table)).add("table", table).add("old", oldColumn).add("new", newColumn).render();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("renameColumn error,sql:{}", sql);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dropColumn(Connection conn, String table, List<String> columns) {

        StringBuilder drop = new StringBuilder();
        columns.forEach(col -> {
            //删除默认值约束
            dropConstraint(conn, table, col);
            String dropLoop = new ST(DROP_COLUMN_LOOP).add("column", col).render();
            drop.append(dropLoop).append(",");
        });
        drop.deleteCharAt(drop.length() - 1);
        String sql = new ST(DROP_COLUMN).add("schema", schemaMap.get(table)).add("table", table).add("drop", drop).render();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("dropColumn error sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    private void dropConstraint(Connection conn, String table, String col) {
        //查询约束名称
        String constraintName = "";
        String sql = new ST(GET_DEFAULT_CONSTRAINT_SQL)
                .add("schema", schemaMap.get(table)
                ).add("table", table)
                .add("field", col)
                .render();
        log.info("getConstraint sql:{}", sql);
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                constraintName = resultSet.getString("ConstraintName");
                sql = new ST(DROP_CONSTRAINT_SQL)
                        .add("schema", schemaMap.get(table))
                        .add("table", table)
                        .add("constraint", constraintName)
                        .render();
                log.info("dropConstraint sql:{}", sql);
                try (Statement statement = conn.createStatement()) {
                    statement.execute(sql);
                    log.info("drop filed {} constraint {} success", col, constraintName);
                }
            } else {
                log.info("field {} has no default constraint", col);
            }
        } catch (Exception e) {
            log.error("getConstraint error sql:{}", sql, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connectionOperate(Connection conn, DatabaseDTO databaseDTO) {
        databaseDTO.getTableDTOList().forEach(tableDTO -> {
            String schema = tableDTO.getSchema();
            if (!schemaMap.containsKey(schema)) {
                schemaMap.put(tableDTO.getTableName(), schema);
            }
        });
    }

    @Override
    public String getType() {
        return DatabaseTypeEnum.SQLServer.name();
    }
}
