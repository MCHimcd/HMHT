package ww.wangnong.config;

import com.influxdb.v3.client.InfluxDBClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hmht.influxdb", name = "enabled", havingValue = "true")
public class InfluxDbConfig {

    @SuppressWarnings("null")
    @Bean
    public InfluxDBClient influxDBClient(InfluxDbProperties properties) {
        return InfluxDBClient.getInstance(
                properties.getUrl(),
                properties.getToken().toCharArray(),
                properties.getDatabase());
    }
}
