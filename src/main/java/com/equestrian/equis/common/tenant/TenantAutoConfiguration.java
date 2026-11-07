package com.equestrian.equis.common.tenant;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

// The database side of multi-tenancy: every connection knows its club, and Hibernate filters by club.
// An auto-configuration (listed in META-INF/spring/...AutoConfiguration.imports) rather than a component, so the
// @DataJpaTest slice can load it too (test resources list it for that slice).
@AutoConfiguration
public class TenantAutoConfiguration {
    // Static: a BeanPostProcessor must not wait for this class to be created.
    @Bean
    static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    // Flyway takes the pool without the wrapper: the wrapper switches to equis_app, which V24 only creates, and
    // migrations must work as the owner anyway. spring.flyway.init-sqls resets a role left on a pooled connection.
    @Bean
    FlywayConfigurationCustomizer flywayWithoutTenant(DataSource dataSource) {
        return configuration -> {
            if (dataSource instanceof TenantAwareDataSource tenantAware) {
                configuration.dataSource(tenantAware.getTargetDataSource());
            }
        };
    }

    @Bean
    HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer() {
        TenantIdentifierResolver resolver = new TenantIdentifierResolver();
        return properties -> properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
