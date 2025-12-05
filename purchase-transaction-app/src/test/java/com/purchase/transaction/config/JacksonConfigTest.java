package com.purchase.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JacksonConfig Tests")
class JacksonConfigTest {
    
    private JacksonConfig jacksonConfig;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        jacksonConfig = new JacksonConfig();
        objectMapper = jacksonConfig.objectMapper();
    }
    
    @Test
    @DisplayName("Should create ObjectMapper bean")
    void testObjectMapperBean() {
        assertNotNull(objectMapper);
    }
    
    @Test
    @DisplayName("Should serialize and deserialize LocalDate")
    void testLocalDateSerialization() throws Exception {
        LocalDate testDate = LocalDate.of(2025, 12, 4);
        String json = objectMapper.writeValueAsString(testDate);
        
        assertNotNull(json);
        assertTrue(json.contains("2025-12-04"));
        
        LocalDate deserialized = objectMapper.readValue(json, LocalDate.class);
        assertEquals(testDate, deserialized);
    }
    
    @Test
    @DisplayName("Should handle null values correctly")
    void testNullHandling() throws Exception {
        TestObject testObj = new TestObject();
        testObj.setValue(null);
        
        String json = objectMapper.writeValueAsString(testObj);
        assertNotNull(json);
        
        TestObject deserialized = objectMapper.readValue(json, TestObject.class);
        assertNull(deserialized.getValue());
    }
    
    // Simple test class
    static class TestObject {
        private String value;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
}
