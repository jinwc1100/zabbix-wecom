package com.sla.notifier.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewEventSchemaMapperTest {

    @Test
    void createAndMigrationDefineLongTextWithoutRemark() throws Exception {
        String schema = read("src/main/resources/mapper/SlaSchemaMapper.xml");
        String create = schema.substring(schema.indexOf("<update id=\"createReviewEventTable\">"),
                schema.indexOf("</update>", schema.indexOf("<update id=\"createReviewEventTable\">")));

        assertTrue(create.contains("resolution_process TEXT NULL"));
        assertTrue(create.contains("root_cause         TEXT NULL"));
        assertTrue(create.contains("improvement        TEXT NULL"));
        assertTrue(create.contains("exclude_time      DATETIME NULL"));
        assertFalse(create.contains("remark"));
        assertTrue(schema.contains("ADD COLUMN resolution_process TEXT NULL"));
        assertTrue(schema.contains("ADD COLUMN exclude_time DATETIME NULL"));
        assertTrue(schema.contains("DROP COLUMN remark"));
        assertTrue(schema.contains("createSystemOwnerTable"));
        assertTrue(schema.contains("createSliSnapshotTable"));
        assertTrue(schema.contains("uk_sli_snapshot"));
        assertTrue(schema.contains("UNIQUE KEY uk_system_owner (system_level, system_name)"));
        assertTrue(schema.contains("INSERT IGNORE INTO zabbix.sla_system_owner"));
        assertTrue(schema.contains("monitor_edition VARCHAR(16) NOT NULL DEFAULT '老版监控'"));
        assertTrue(schema.contains("write_jdbc_key VARCHAR(16) NOT NULL DEFAULT 'zabbix6'"));
        assertTrue(schema.contains("ADD COLUMN monitor_edition VARCHAR(16) NOT NULL DEFAULT '老版监控'"));
        assertTrue(schema.contains("ADD COLUMN write_jdbc_key VARCHAR(16) NOT NULL DEFAULT 'zabbix6'"));
        assertFalse(schema.contains("zabbix7."));
    }

    @Test
    void reviewUpdateDoesNotChangeMachineColumns() throws Exception {
        String mapper = read("src/main/resources/mapper/SlaReviewEventMapper.xml");
        String update = mapper.substring(mapper.indexOf("<update id=\"updateReviewFields\">"),
                mapper.indexOf("</update>", mapper.indexOf("<update id=\"updateReviewFields\">")));

        assertTrue(update.contains("resolution_process ="));
        assertTrue(update.contains("root_cause ="));
        assertTrue(update.contains("improvement ="));
        assertTrue(update.contains("exclude_time ="));
        assertTrue(update.contains("DATE_ADD(down_start, INTERVAL 1 SECOND)"));
        assertFalse(update.contains("LEAST"));
        assertFalse(update.contains("system_name ="));
        assertFalse(update.contains("down_start ="));
        assertFalse(update.contains("owner_name ="));
    }

    @Test
    void refreshExcludeTimeUpdatesOnlyDerivedColumn() throws Exception {
        String mapper = read("src/main/resources/mapper/SlaReviewEventMapper.xml");
        String update = mapper.substring(mapper.indexOf("<update id=\"refreshExcludeTime\">"),
                mapper.indexOf("</update>", mapper.indexOf("<update id=\"refreshExcludeTime\">")));

        assertTrue(update.contains("exclude_time ="));
        assertTrue(update.contains("count_sla = '是'"));
        assertTrue(update.contains("count_sla = '否'"));
        assertTrue(update.contains("DATE_ADD(down_start, INTERVAL 1 SECOND)"));
        assertFalse(update.contains("LEAST"));
        assertFalse(update.contains("owner_name"));
        assertFalse(update.contains("INSERT"));
        assertFalse(update.contains("down_start ="));
    }

    @Test
    void ownerRefreshUpdatesOnlyOwnerName() throws Exception {
        String mapper = read("src/main/resources/mapper/SlaReviewEventMapper.xml");
        String update = mapper.substring(mapper.indexOf("<update id=\"updateOwnerByIdentity\">"),
                mapper.indexOf("</update>", mapper.indexOf("<update id=\"updateOwnerByIdentity\">")));

        assertTrue(update.contains("owner_name = #{ownerName}"));
        assertFalse(update.contains("AND down_end"));
        assertFalse(update.contains("fault_category"));
        assertFalse(update.contains("root_cause"));
        assertFalse(update.contains("count_sla"));
        assertFalse(update.contains("INSERT"));
    }

    @Test
    void listSupportsMonthRangeAndOwnerSystemFilters() throws Exception {
        String mapper = read("src/main/resources/mapper/SlaReviewEventMapper.xml");
        String filters = mapper.substring(mapper.indexOf("<sql id=\"reviewEventFilters\">"),
                mapper.indexOf("</sql>", mapper.indexOf("<sql id=\"reviewEventFilters\">")));
        String list = mapper.substring(mapper.indexOf("<select id=\"listReviewEvents\""),
                mapper.indexOf("</select>", mapper.indexOf("<select id=\"listReviewEvents\"")));

        assertTrue(filters.contains("LOCATE(#{system}"));
        assertTrue(filters.contains("LOCATE(#{owner}"));
        assertTrue(filters.contains("period_month = #{periodMonth}"));
        assertTrue(filters.contains("down_start &gt;= #{rangeStart}"));
        assertTrue(filters.contains("reviewStatus == 'incomplete'"));
        assertTrue(filters.contains("system_level = #{systemLevel}"));
        assertTrue(filters.contains("ownerEmptyOnly"));
        assertTrue(filters.contains("incompleteCountSla"));
        assertTrue(filters.contains("count_sla IN ('是', '否')"));
        assertTrue(filters.contains("count_sla = #{countSla}"));
        assertTrue(mapper.contains("listReviewedEvents"));
        assertTrue(mapper.contains("count_sla IN ('是', '否')"));
        assertTrue(list.contains("LIMIT #{size} OFFSET #{offset}"));
        assertFalse(list.contains("exclude_time"));
        assertFalse(mapper.contains("LOCATE(#{time}"));
    }

    @Test
    void incompleteQueriesUseEmptyCountSlaOnly() throws Exception {
        String mapper = read("src/main/resources/mapper/SlaReviewEventMapper.xml");
        String incomplete = mapper.substring(mapper.indexOf("<sql id=\"incompleteCountSla\">"),
                mapper.indexOf("</sql>", mapper.indexOf("<sql id=\"incompleteCountSla\">")));
        String count = mapper.substring(mapper.indexOf("<select id=\"countIncompleteReviewEvents\""),
                mapper.indexOf("</select>", mapper.indexOf("<select id=\"countIncompleteReviewEvents\"")));
        String names = mapper.substring(mapper.indexOf("<select id=\"listIncompleteOwnerNames\""),
                mapper.indexOf("</select>", mapper.indexOf("<select id=\"listIncompleteOwnerNames\"")));

        assertTrue(incomplete.contains("count_sla IS NULL"));
        assertTrue(incomplete.contains("TRIM(count_sla) = ''"));
        assertFalse(incomplete.contains("是"));
        assertFalse(incomplete.contains("period_month"));
        assertTrue(count.contains("COUNT(*)"));
        assertTrue(names.contains("DISTINCT owner_name"));
        assertFalse(count.contains("period_month"));
        assertFalse(names.contains("period_month"));
    }

    @Test
    void overviewAggregationsCountRowsWithoutSystemDistinct() throws Exception {
        String mapper = read("src/main/resources/mapper/SlaReviewEventMapper.xml");
        String level = mapper.substring(mapper.indexOf("<select id=\"countIncompleteBySystemLevel\""),
                mapper.indexOf("</select>", mapper.indexOf("<select id=\"countIncompleteBySystemLevel\"")));
        String owners = mapper.substring(mapper.indexOf("<select id=\"countIncompleteByOwner\""),
                mapper.indexOf("</select>", mapper.indexOf("<select id=\"countIncompleteByOwner\"")));

        assertTrue(level.contains("COUNT(*)"));
        assertTrue(level.contains("system_level = #{systemLevel}"));
        assertFalse(level.toUpperCase().contains("DISTINCT"));
        assertTrue(owners.contains("COUNT(*) AS count"));
        assertTrue(owners.contains("ORDER BY count DESC"));
        assertFalse(level.contains("period_month"));
        assertFalse(owners.contains("period_month"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
