package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/6/24 13:47
 */
@Slf4j
public class Mysql57Execute extends MysqlExecute {

    @Override
    public String getType() {
        return DatabaseTypeEnum.MySQL57.name();
    }

    @Override
    public void renameColumn(Connection conn, String schema, String table, String oldColumn, String newColumn) {
        String sql = "DESCRIBE " + table + " " + oldColumn;
        try (Statement statement = conn.createStatement()) {
            //查询旧列信息
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                String columnType = rs.getString("Type");
                String alterTableQuery = "ALTER TABLE " + table + " CHANGE " + oldColumn + " " + newColumn + " " + columnType;
                //修改表结构
                statement.executeUpdate(alterTableQuery);
                log.info("renameColumn success sql:{}", alterTableQuery);
            }
        } catch (Exception e) {
            log.error("renameColumn error sql:{}", sql);
            throw new RuntimeException(e);
        }
    }
}
