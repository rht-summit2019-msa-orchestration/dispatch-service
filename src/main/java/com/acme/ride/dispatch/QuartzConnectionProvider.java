package com.acme.ride.dispatch;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.quartz.utils.ConnectionProvider;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class QuartzConnectionProvider implements ConnectionProvider {

    private String dataSourceName;

    private DataSource dataSource;

    @Override
    public Connection getConnection() throws SQLException {

        try {
            if (dataSource == null) {
                dataSource = SpringContext.getBean(dataSourceName, DataSource.class);
            }

            if(dataSource == null) {
                throw new SQLException( "There is no bean with name '" + dataSource + "'");
            }

            if (dataSource instanceof XADataSource) {
                return DataSourceUtils.doGetConnection(dataSource);
            } else {
                return dataSource.getConnection();
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void shutdown() throws SQLException {

    }

    @Override
    public void initialize() throws SQLException {

    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }
}
