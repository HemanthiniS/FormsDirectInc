package com.formsdirectinc.environment;

import com.formsdirectinc.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("customEnvironment")
public class Environment {

    @Autowired
    private org.springframework.core.env.Environment springEnvironment;

    public String[] getActiveProfiles() {
        return springEnvironment.getActiveProfiles();
    }

    public String[] getDefaultProfiles() {
        return springEnvironment.getDefaultProfiles();
    }

    public boolean acceptsProfiles(String... profiles) {
        return springEnvironment.acceptsProfiles(profiles);
    }

    public boolean containsProperty(String key) {
        return springEnvironment.containsProperty(getPrefixedKey(key));
    }

    public String getProperty(String key) {
        return getValue(key);
    }

    public String getProperty(String key, String defaultValue) {
        return getValue(key, defaultValue);
    }

    public <T> T getProperty(String key, Class<T> targetType) {
        return getValue(key, targetType);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return getValue(key, targetType, defaultValue);
    }

    public String getRequiredProperty(String key) {
        return springEnvironment.getRequiredProperty(getPrefixedKey(key));
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        return springEnvironment.getRequiredProperty(getPrefixedKey(key), targetType);
    }

    public String resolvePlaceholders(String text) {
        return springEnvironment.resolvePlaceholders(text);
    }

    public String resolveRequiredPlaceholders(String text) {
        return springEnvironment.resolveRequiredPlaceholders(text);
    }

    private <T> T getValue(String key, Class<T> targetType, T defaultValue) {
        T result = springEnvironment.getProperty(getPrefixedKey(key), targetType);
        if(result == null) {
            result = springEnvironment.getProperty(key, targetType, defaultValue);
        }
        return result;
    }

    private <T> T getValue(String key, Class<T> targetType) {
        T result = springEnvironment.getProperty(getPrefixedKey(key), targetType);
        if (result == null) {
            result = springEnvironment.getProperty(key, targetType);
        }
        return result;
    }

    private String getValue(String key) {
        String result = springEnvironment.getProperty(getPrefixedKey(key));
        if (result == null) {
            result = springEnvironment.getProperty(key);
        }
        return result;
    }

    private String getValue(String key, String defaultValue) {
        String result = springEnvironment.getProperty(getPrefixedKey(key));
        if (result == null) {
            result = springEnvironment.getProperty(key, defaultValue);
        }
        return result;
    }

    private String getPrefixedKey(String key) {
        try {
            String tenantId = TenantContextHolder.getTenantId();
            List<String> commonKeysPrefixList = Arrays.asList(springEnvironment.getProperty(
                    "fd.common.keys.prefix", "spring,server,site").split(","));

            if (tenantId == null || tenantId.isEmpty() || commonKeysPrefixList.stream().anyMatch(key:: startsWith)) {
                return key;
            } else {
                return tenantId + "." + key;
            }
        } catch (NullPointerException e) {
            return key;
        }
    }

    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getValue(key));
    }

    public int getIntegerProperty(String key) {
        return Integer.parseInt(getValue(key));
    }

    public float getFloatProperty(String key) {
        return Float.parseFloat(getValue(key));
    }

    public boolean getBooleanPropertyOrDefault(String key, Boolean defaultValue) {
        return Boolean.parseBoolean(getValue(key, defaultValue.toString()));
    }

    public int getIntegerPropertyOrDefault(String key, Integer defaultValue) {
        return Integer.parseInt(getValue(key, defaultValue.toString()));
    }

    public float getFloatPropertyOrDefault(String key, Float defaultValue) {
        return Float.parseFloat(getValue(key, defaultValue.toString()));
    }

    public String[] getProperties(String key) {
        String property = getValue(key);
        if(property == null) {
            return null;
        }
        String [] propertyList = property.replace("{", "").replace("}", "").split(",");
        return propertyList;
    }

    public <T> List<T> getPropertiesAsList(String key) {
        String property = getValue(key);
        if(property == null) {
            return null;
        }
        T [] propertyList = (T[]) property.replace("{", "").replace("}", "").split(",");
        return Arrays.asList(propertyList);
    }

    public String[] getPropertiesOrDefault(String key, String[] defaultValue) {
        try {
            String[] propertyList = getProperties(key);
            return (propertyList == null || propertyList.length == 0) ? defaultValue : propertyList;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public <T> List<T> getPropertiesAsListOrDefault(String key, List<T> defaultValue) {
        try {
            String property = getValue(key);
            if(property == null) {
                return defaultValue;
            }
            T[] propertyList = (T[]) property.replace("{", "").replace("}", "").split(",");
            return (propertyList == null || propertyList.length == 0) ? defaultValue : Arrays.asList(propertyList);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String[] getActiveSites() {
        String sites = springEnvironment.getProperty("active.sites");
        if (sites != null) {
            return sites.split(",");
        }
        return null;
    }

    public String getPropertyForKey(String key) {
        return springEnvironment.getProperty(key);
    }

    public String getPropertyForKey(String key, String defaultValue) {
        return springEnvironment.getProperty(key, defaultValue);
    }
}
