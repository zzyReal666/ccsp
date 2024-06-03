package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/5/29 15:23
 */
public class OracleExecute implements SqlExecuteSPI {
    @Override
    public String getPrimaryKey(Connection conn, String schema, String table) throws SQLException {
        return "";
    }

    @Override
    public String getTempColumnSuffix() {
        return "";
    }

    @Override
    public void addTempColumn(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList) {

    }

    @Override
    public int count(Connection conn, String table) {
        return 0;
    }

    @Override
    public List<Map<String, String>> selectColumn(Connection conn, String table, List<String> columns, int limit, int offset) {
        return null;
    }

    @Override
    public void batchUpdate(Connection conn, String table, List<Map<String, String>> data,int limit,int offset) {

    }

    @Override
    public void renameColumn(Connection conn, String schema, String table, String oldColumn, String newColumn) {

    }

    @Override
    public void dropColumn(Connection conn, String table, List<String> columns) {

    }

    @Override
    public String getType() {
        return DatabaseTypeEnum.Oracle.name();
    }
}
