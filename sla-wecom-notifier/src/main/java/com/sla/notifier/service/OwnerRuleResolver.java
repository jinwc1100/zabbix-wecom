package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.SlaSystemOwner;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按 (system_level, system_name) 查 sla_system_owner。
 */
@Service
public class OwnerRuleResolver {

    private final SlaSystemOwnerMapper mapper;

    public OwnerRuleResolver(SlaSystemOwnerMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, SlaSystemOwner> loadSnapshot() {
        List<SlaSystemOwner> rows = mapper.selectAll();
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, SlaSystemOwner> map = new HashMap<String, SlaSystemOwner>();
        for (SlaSystemOwner row : rows) {
            if (row == null) {
                continue;
            }
            map.put(key(row.getSystemLevel(), row.getSystemName()), row);
        }
        return map;
    }

    public ResolvedOwner resolve(String systemLevel, String systemName) {
        return resolve(systemLevel, systemName, loadSnapshot());
    }

    public ResolvedOwner resolve(String systemLevel, String systemName,
                                 Map<String, SlaSystemOwner> snapshot) {
        if (snapshot == null) {
            return unmatched();
        }
        SlaSystemOwner row = snapshot.get(key(systemLevel, systemName));
        if (row == null) {
            return unmatched();
        }
        String owner = row.getOwnerName();
        String mobile = row.getMobile();
        boolean matched = owner != null && !owner.trim().isEmpty();
        return new ResolvedOwner(
                matched ? owner.trim() : null,
                mobile == null || mobile.trim().isEmpty() ? null : mobile.trim(),
                matched);
    }

    static String key(String systemLevel, String systemName) {
        String level = systemLevel == null ? "" : systemLevel.trim();
        String name = systemName == null ? "" : systemName.trim();
        return level + '\0' + name;
    }

    private ResolvedOwner unmatched() {
        return new ResolvedOwner(null, null, false);
    }

    public static class ResolvedOwner {
        private final String name;
        private final String mobile;
        private final boolean matched;

        public ResolvedOwner(String name, String mobile, boolean matched) {
            this.name = name;
            this.mobile = mobile;
            this.matched = matched;
        }

        public String getName() {
            return name;
        }

        public String getMobile() {
            return mobile;
        }

        public boolean isMatched() {
            return matched;
        }
    }
}
