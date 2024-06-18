package com.spms.dbhsm.stockDataProcess.service;

import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.sqlExecute.SqlExecuteSPI;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Connection;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/6/18 15:51
 */
@Data
@AllArgsConstructor
public class OperateContext {

    private boolean encrypt;
    private SqlExecuteSPI sqlExecute;
    private Connection dbaConn;
    private DatabaseDTO databaseDTO;
    private String primaryKey;
    private int offset;

}
