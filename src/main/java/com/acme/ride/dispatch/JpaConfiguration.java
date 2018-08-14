package com.acme.ride.dispatch;

import java.util.HashMap;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
public class JpaConfiguration {

    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    @Qualifier("xaDataSource")
    private DataSource xaDataSource;
    
    @Bean
    public PersistenceUnitManager persistenceUnitManager() {
        DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
        persistenceUnitManager.setDefaultDataSource(xaDataSource);
        persistenceUnitManager.setPersistenceXmlLocation("classpath:/META-INF/jbpm-persistence.xml");
        return persistenceUnitManager;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        AbstractJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(jpaProperties.isShowSql());
        adapter.setDatabase(jpaProperties.determineDatabase(xaDataSource));
        adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        return adapter;
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(PersistenceUnitManager persistenceUnitManager,
            JpaVendorAdapter jpaVendorAdapter) {
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.putAll(jpaProperties.getProperties());

        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setJpaVendorAdapter(jpaVendorAdapter);
        emfBean.setPersistenceUnitManager(persistenceUnitManager);
        emfBean.setJpaPropertyMap(properties);
        return emfBean;
    }
    
    @Bean
    public EntityManagerFactory entityManagerFactory(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return entityManagerFactory.getObject();
    }
}
