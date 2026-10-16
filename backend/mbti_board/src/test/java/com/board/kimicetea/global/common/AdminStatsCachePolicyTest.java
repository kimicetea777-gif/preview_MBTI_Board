package com.board.kimicetea.global.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AdminStatsCachePolicyTest {

    @Test
    void 캐시_TTL은_갱신_주기보다_길다() {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        final Properties props = yaml.getObject();

        final long refreshDelayMs = Long.parseLong(props.getProperty("admin.stats.cache-refresh-delay-ms"));

        assertThat(Duration.ofHours(AdminStatsCachePolicy.TTL_HOURS))
                .isGreaterThan(Duration.ofMillis(refreshDelayMs));
    }
}
