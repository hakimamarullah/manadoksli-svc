package com.manadoksli.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manadoksli.aspect.annotation.Censor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.io.Serial;
import java.util.List;
import java.util.TimeZone;

@Configuration
public class MapperConfig {

    // ── Censors ───────────────────────────────────────────────────────────────

    protected static class CensorSerializer extends ValueSerializer<Object> {

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            if (value instanceof String str) {
                int len = str.length();
                if (len > 40) {
                    gen.writeString("[REDACTED]");
                    return;
                }
                if (len == 1) {
                    gen.writeString("*");
                    return;
                }
                if (len == 2) {
                    gen.writeString("**");
                    return;
                }
                if (len == 3) {
                    gen.writeString(str.charAt(0) + "**");
                    return;
                }
                gen.writeString(str.charAt(0) + "*".repeat(len - 2) + str.charAt(len - 1));
                return;
            }
            gen.writeString("********");
        }
    }

    protected static class FullCensorSerializer extends ValueSerializer<Object> {

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("********");
        }
    }

    // ── Modifier ──────────────────────────────────────────────────────────────

    protected static class CensorBeanSerializerModifier extends ValueSerializerModifier {

        @Serial
        private static final long serialVersionUID = -478014894636159153L;

        private final ValueSerializer<Object> censorSerializer = new CensorSerializer();
        private final ValueSerializer<Object> fullCensorSerializer = new FullCensorSerializer();

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription.Supplier beanDesc,
                                                         List<BeanPropertyWriter> beanProperties) {
            for (BeanPropertyWriter writer : beanProperties) {
                if (writer.getMember().hasAnnotation(Censor.class)) {
                    Censor censorAnnotation = writer.getMember().getAnnotation(Censor.class);
                    writer.assignSerializer(censorAnnotation.full() ? fullCensorSerializer : censorSerializer);
                }
            }
            return beanProperties;
        }

    }

    // ── Beans ─────────────────────────────────────────────────────────────────

    @Bean("loggingMapper")
    public JsonMapper loggingMapper() {
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new CensorBeanSerializerModifier());
        return JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .changeDefaultPropertyInclusion(it -> it.withValueInclusion(JsonInclude.Include.NON_NULL))
                .defaultTimeZone(TimeZone.getDefault())
                .addModule(module)
                .build();
    }

    @Bean
    @Primary
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultTimeZone(TimeZone.getDefault())
                .build();
    }
}