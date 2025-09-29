package com.ebingo.backend.system.config;


import com.ebingo.backend.system.converters.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

import java.util.List;

@Configuration
@EnableR2dbcAuditing(auditorAwareRef = "auditorAware") // reactive auditing for R2DBC
@RequiredArgsConstructor
public class DatabaseConfig extends AbstractR2dbcConfiguration {

    @Autowired
    ConnectionFactory connectionFactory;

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    private final ObjectMapper objectMapper;

    @Override
    protected List<Object> getCustomConverters() {
        return List.of(new IntegerListToJsonConverter(objectMapper),
                new JsonToIntegerListConverter(objectMapper),
                new JsonToLongListConverter(objectMapper),
                new JsonToStringListConverter(objectMapper),
                new LongListToJsonConverter(objectMapper),
                new StringListToJsonConverter(objectMapper));
    }


//    @Bean
//    public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper) {
//        return R2dbcCustomConversions.of(
//                PostgresDialect.INSTANCE,
//                List.of(
//                        new IntegerListToJsonConverter(objectMapper),
//                        new JsonToIntegerListConverter(objectMapper),
//                        new JsonToLongListConverter(objectMapper),
//                        new JsonToStringListConverter(objectMapper),
//                        new LongListToJsonConverter(objectMapper),
//                        new StringListToJsonConverter(objectMapper)
//                )
//        );
//    }
}
