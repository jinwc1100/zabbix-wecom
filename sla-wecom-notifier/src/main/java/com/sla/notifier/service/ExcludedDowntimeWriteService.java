package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.SlaSystemOwner;
import com.sla.notifier.util.TimeFormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ExcludedDowntimeWriteService {

    private static final Logger log = LoggerFactory.getLogger(ExcludedDowntimeWriteService.class);

    static final String ZABBIX6 = "zabbix6";
    static final String ZABBIX7 = "zabbix7";

    private static final String SQL_SLAID = "SELECT slaid FROM sla WHERE name = ?";
    static final String SQL_OVERLAP_COUNT =
            "SELECT COUNT(*) FROM sla_excluded_downtime WHERE slaid = ? AND period_to > ? AND period_from < ?";
    static final String SQL_IDS_BY_START =
            "SELECT sla_excluded_downtimeid FROM sla_excluded_downtime WHERE slaid = ? AND period_from = ? "
                    + "ORDER BY sla_excluded_downtimeid";
    private static final String SQL_NEXT_ID =
            "SELECT nextid FROM ids WHERE table_name = ? AND field_name = ?";
    private static final String SQL_UPDATE_ID =
            "UPDATE ids SET nextid = ? WHERE table_name = ? AND field_name = ?";
    private static final String SQL_INSERT =
            "INSERT INTO sla_excluded_downtime (sla_excluded_downtimeid, slaid, name, period_from, period_to) "
                    + "VALUES (?, ?, ?, ?, ?)";
    static final String SQL_UPDATE_WINDOW =
            "UPDATE sla_excluded_downtime SET name = ?, period_to = ? WHERE sla_excluded_downtimeid = ?";
    static final String SQL_UPDATE_PERIOD_TO_ONLY =
            "UPDATE sla_excluded_downtime SET period_to = ? WHERE sla_excluded_downtimeid = ?";
    static final String SQL_DELETE_BY_ID =
            "DELETE FROM sla_excluded_downtime WHERE sla_excluded_downtimeid = ?";
    private static final String IDS_TABLE = "sla_excluded_downtime";
    private static final String IDS_FIELD = "sla_excluded_downtimeid";

    private final SlaSystemOwnerMapper ownerMapper;
    private final JdbcTemplate zabbix6Jdbc;
    private final JdbcTemplate zabbix7Jdbc;

    public ExcludedDowntimeWriteService(SlaSystemOwnerMapper ownerMapper,
                                        JdbcTemplate zabbix6Jdbc,
                                        @Qualifier("zabbix7JdbcTemplate") JdbcTemplate zabbix7Jdbc) {
        this.ownerMapper = ownerMapper;
        this.zabbix6Jdbc = zabbix6Jdbc;
        this.zabbix7Jdbc = zabbix7Jdbc;
    }

    public void writeAfterSave(SlaReviewEvent event) {
        if (event == null) {
            throw fail("复盘记录不存在");
        }
        String level = trim(event.getSystemLevel());
        String systemName = trim(event.getSystemName());
        if (level.isEmpty() || systemName.isEmpty()) {
            throw fail("复盘系统名称不能为空");
        }
        SlaSystemOwner owner = ownerMapper.selectByLevelAndName(level, systemName);
        if (owner == null || isBlank(owner.getWriteJdbcKey())) {
            throw fail("未找到系统写库标记");
        }
        String key = owner.getWriteJdbcKey().trim();
        JdbcTemplate jdbc = jdbcFor(key);
        Long slaid = findSlaid(jdbc, systemName);
        if (slaid == null) {
            throw fail("未找到对应 SLA");
        }
        if (event.getDownStart() == null || event.getExcludeTime() == null) {
            throw fail("排除时间不能为空");
        }
        int periodFrom = toUnix(event.getDownStart());
        int periodTo = toUnix(event.getExcludeTime());
        List<Long> existing = jdbc.queryForList(SQL_IDS_BY_START, Long.class,
                slaid, Integer.valueOf(periodFrom));
        if (existing == null || existing.isEmpty()) {
            long id = allocateId(jdbc);
            jdbc.update(SQL_INSERT, Long.valueOf(id), slaid, exclusionName(event),
                    Integer.valueOf(periodFrom), Integer.valueOf(periodTo));
            return;
        }
        Long keepId = existing.get(0);
        jdbc.update(SQL_UPDATE_WINDOW, exclusionName(event), Integer.valueOf(periodTo), keepId);
        for (int i = 1; i < existing.size(); i++) {
            jdbc.update(SQL_DELETE_BY_ID, existing.get(i));
        }
    }

    /**
     * Sync path: UPDATE period_to only on existing exclusion rows. Never INSERT.
     * Failures are logged and return 0 so the whole sync is not aborted.
     *
     * @return number of exclusion rows updated
     */
    public int updatePeriodToAfterSync(SlaReviewEvent event) {
        try {
            if (event == null || event.getDownStart() == null || event.getExcludeTime() == null) {
                return 0;
            }
            if (!isNoSla(event.getCountSla())) {
                return 0;
            }
            String level = trim(event.getSystemLevel());
            String systemName = trim(event.getSystemName());
            if (level.isEmpty() || systemName.isEmpty()) {
                return 0;
            }
            SlaSystemOwner owner = ownerMapper.selectByLevelAndName(level, systemName);
            if (owner == null || isBlank(owner.getWriteJdbcKey())) {
                log.warn("同步排除 period_to 跳过：无写库标记 level={} name={}", level, systemName);
                return 0;
            }
            JdbcTemplate jdbc = jdbcFor(owner.getWriteJdbcKey().trim());
            Long slaid = findSlaid(jdbc, systemName);
            if (slaid == null) {
                log.warn("同步排除 period_to 跳过：无 SLA name={}", systemName);
                return 0;
            }
            int periodFrom = toUnix(event.getDownStart());
            int periodTo = toUnix(event.getExcludeTime());
            List<Long> existing = jdbc.queryForList(SQL_IDS_BY_START, Long.class,
                    slaid, Integer.valueOf(periodFrom));
            if (existing == null || existing.isEmpty()) {
                return 0;
            }
            int updated = 0;
            for (Long id : existing) {
                updated += jdbc.update(SQL_UPDATE_PERIOD_TO_ONLY,
                        Integer.valueOf(periodTo), id);
            }
            return updated;
        } catch (Exception e) {
            log.warn("同步更新排除 period_to 失败 level={} name={}: {}",
                    event == null ? null : event.getSystemLevel(),
                    event == null ? null : event.getSystemName(),
                    e.toString());
            return 0;
        }
    }

    JdbcTemplate jdbcFor(String writeJdbcKey) {
        if (ZABBIX6.equals(writeJdbcKey)) {
            return zabbix6Jdbc;
        }
        if (ZABBIX7.equals(writeJdbcKey)) {
            return zabbix7Jdbc;
        }
        throw fail("写库标记无效");
    }

    static int toUnix(java.time.LocalDateTime value) {
        return (int) value.atZone(TimeFormatUtil.ZONE).toEpochSecond();
    }

    static String exclusionName(SlaReviewEvent event) {
        String name = "故障分类:" + nullToEmpty(event.getFaultCategory())
                + "--故障子类:" + nullToEmpty(event.getFaultSubcategory())
                + "--故障描述:" + nullToEmpty(event.getFaultDesc());
        if (name.length() > 255) {
            return name.substring(0, 255);
        }
        return name;
    }

    Long findSlaid(JdbcTemplate jdbc, String systemName) {
        try {
            return jdbc.queryForObject(SQL_SLAID, Long.class, systemName);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    boolean hasGrafanaOverlap(JdbcTemplate jdbc, long slaid, int downStartUnix, int downEndUnix) {
        Long count = jdbc.queryForObject(SQL_OVERLAP_COUNT, Long.class,
                Long.valueOf(slaid), Integer.valueOf(downStartUnix), Integer.valueOf(downEndUnix));
        return count != null && count.longValue() > 0L;
    }

    private long allocateId(JdbcTemplate jdbc) {
        Long nextid = jdbc.queryForObject(SQL_NEXT_ID, Long.class, IDS_TABLE, IDS_FIELD);
        if (nextid == null) {
            throw fail("无法分配排除时间主键");
        }
        long id = nextid.longValue() + 1L;
        int updated = jdbc.update(SQL_UPDATE_ID, Long.valueOf(id), IDS_TABLE, IDS_FIELD);
        if (updated != 1) {
            throw fail("无法分配排除时间主键");
        }
        return id;
    }

    private static boolean isNoSla(String countSla) {
        return countSla != null && "否".equals(countSla.trim());
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static ResponseStatusException fail(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
