package org.springframework.boot.jta.narayana;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbcpXADataSourceWrapper implements XADataSourceWrapper {

    @Autowired
    private NarayanaRecoveryManagerBean recoveryManager;

    @Autowired
    private TransactionManager tm;

    @Override
    public DataSource wrapDataSource(XADataSource xaDataSource) throws Exception {
        DataSourceXAResourceRecoveryHelper helper = new DataSourceXAResourceRecoveryHelper(xaDataSource);
        recoveryManager.registerXAResourceRecoveryHelper(helper);
        DataSourceXAConnectionFactory dataSourceXAConnectionFactory = new DataSourceXAConnectionFactory(tm, xaDataSource);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(dataSourceXAConnectionFactory, null);
        GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);
        return new ManagedDataSource<>(connectionPool, dataSourceXAConnectionFactory.getTransactionRegistry());
    }
}
