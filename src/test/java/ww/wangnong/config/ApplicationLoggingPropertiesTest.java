package ww.wangnong.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ApplicationLoggingPropertiesTest {

    @Test
    void mainApplicationProperties_sqlLoggingDisabled_expectedValues() throws Exception {
        Path propertiesPath = Path.of("src", "main", "resources", "application.properties");
        assertThat(Files.exists(propertiesPath)).isTrue();

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            properties.load(in);
        }

        assertThat(properties.getProperty("spring.jpa.show-sql")).isEqualTo("false");
        assertThat(properties.getProperty("spring.jpa.properties.hibernate.format_sql")).isEqualTo("false");
        assertThat(properties.getProperty("logging.level.org.hibernate.SQL")).isEqualTo("INFO");
        assertThat(properties.getProperty("logging.level.org.hibernate.orm.jdbc.bind")).isEqualTo("INFO");
    }

    @Test
    void buildIncludesFlywayBootAutoConfigurationModule() throws Exception {
        String buildFile = Files.readString(Path.of("build.gradle"));

        assertThat(buildFile).contains("org.springframework.boot:spring-boot-flyway");
        assertThat(buildFile).contains("org.flywaydb:flyway-core");
        assertThat(buildFile).contains("org.flywaydb:flyway-mysql");
    }
}
