package com.sla.notifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "owners")
public class OwnerProperties {

    private String defaultOwner = "待指定责任人";

    /**
     * 可选：YAML map 形式（中文 key 在部分环境下可能绑定失败，优先用 entries）
     */
    private Map<String, String> mapping = new LinkedHashMap<String, String>();

    /**
     * 推荐：列表形式维护系统责任人
     */
    private List<OwnerEntry> entries = new ArrayList<OwnerEntry>();

    public String getDefaultOwner() {
        return defaultOwner;
    }

    public void setDefaultOwner(String defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    public List<OwnerEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<OwnerEntry> entries) {
        this.entries = entries;
    }

    public String resolveOwner(String systemName) {
        if (systemName == null) {
            return defaultOwner;
        }
        if (entries != null) {
            for (OwnerEntry entry : entries) {
                if (systemName.equals(entry.getSystem())) {
                    return entry.getOwner();
                }
            }
        }
        if (mapping != null && mapping.containsKey(systemName)) {
            String owner = mapping.get(systemName);
            if (owner != null && !owner.trim().isEmpty()) {
                return owner;
            }
        }
        return defaultOwner;
    }

    public static class OwnerEntry {
        private String system;
        private String owner;

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }
    }
}
