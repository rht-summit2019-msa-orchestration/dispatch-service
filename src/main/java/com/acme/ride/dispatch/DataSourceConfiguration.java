package com.acme.ride.dispatch;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Configuration
public class DataSourceConfiguration implements BeanClassLoaderAware {

    @Autowired
    private XADataSourceWrapper wrapper;

    @Autowired
    private DataSourceProperties properties;

    private ClassLoader classLoader;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Bean(name="xaDataSource")
    @Qualifier("xaDataSource")
    @Primary
    public DataSource xaDataSource() throws Exception {
        return wrapper.wrapDataSource(createXaDataSource());
    }

    @Bean(name="nonXaDataSource")
    @Qualifier("nonXaDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.dbcp2")
    public DataSource nonXaDataSource() {
        return properties.initializeDataSourceBuilder().build();
    }

    private XADataSource createXaDataSource() {
        String className = this.properties.getXa().getDataSourceClassName();
        if (!StringUtils.hasLength(className)) {
            className = DatabaseDriver.fromJdbcUrl(this.properties.determineUrl())
                    .getXaDataSourceClassName();
        }
        Assert.state(StringUtils.hasLength(className),
                "No XA DataSource class name specified");
        XADataSource dataSource = createXaDataSourceInstance(className);
        bindXaProperties(dataSource, this.properties);
        return dataSource;
    }

    private XADataSource createXaDataSourceInstance(String className) {
        try {
            Class<?> dataSourceClass = ClassUtils.forName(className, this.classLoader);
            Object instance = BeanUtils.instantiate(dataSourceClass);
            Assert.isInstanceOf(XADataSource.class, instance);
            return (XADataSource) instance;
        }
        catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to create XADataSource instance from '" + className + "'", ex);
        }
    }

    private void bindXaProperties(XADataSource target, DataSourceProperties properties) {
        MutablePropertyValues values = new MutablePropertyValues();
        values.add("user", this.properties.determineUsername());
        values.add("password", this.properties.determinePassword());
        values.add("url", this.properties.determineUrl());
        values.addPropertyValues(properties.getXa().getProperties());
        new RelaxedDataBinder(target).withAlias("user", "username").bind(values);
    }

}
