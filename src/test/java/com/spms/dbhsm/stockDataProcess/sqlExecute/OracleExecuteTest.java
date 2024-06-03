package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.common.spi.typed.TypedSPIRegistry;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.Assert.*;

public class OracleExecuteTest {
    SqlExecuteSPI executeSPI;
    Connection connection;

    @Before
    public void setUp() throws Exception {
        Optional<SqlExecuteSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteSPI.class, DatabaseTypeEnum.Oracle.name());
        assert registeredService.isPresent();
        executeSPI = registeredService.get();
    }

    @Test
    public void getPrimaryKey() {
    }

    @Test
    public void getTempColumnSuffix() {
    }

    @Test
    public void addTempColumn() {
    }

    @Test
    public void count() {
    }

    @Test
    public void selectColumn() {
    }

    @Test
    public void batchUpdate() {
    }

    @Test
    public void renameColumn() {
    }

    @Test
    public void dropColumn() {
    }

    @Test
    public void getType() {
    }
}