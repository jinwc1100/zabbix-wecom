package com.sla.notifier.config;

import com.sla.notifier.mapper.SlaSchemaMapper;
import com.sla.notifier.model.SlaFaultCategory;
import com.sla.notifier.model.SlaSystemOwner;
import com.sla.notifier.service.MonitorEditionBackfillService;
import com.sla.notifier.service.SystemOwnerWorkbookLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

@Component
public class SlaSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SlaSchemaInitializer.class);

    private static final String[][] DICT = {
            {"直接性故障", "代理入口类故障", "如负载均衡、网关", "是"},
            {"直接性故障", "应用类故障", "如应用bug，应用性能导致的故障", "是"},
            {"直接性故障", "配置类故障", "如配置输入错误，配置变更产生的故障", "是"},
            {"直接性故障", "数据库类故障", "凡ES,MYSQL,MONGODB,REDIS等数据库性能导致的故障", "是"},
            {"直接性故障", "中间件类故障", "自建的配置中心、消息中间件，服务发现等导致的故障", "是"},
            {"直接性故障", "安全类故障", "受到外部攻击，或由于安全策略导致自身业务出现问题", "是"},
            {"直接性故障", "发布类故障", "发布周期内（构件、部署、验证）导致的故障", "否"},
            {"直接性故障", "维护期停机", "例行维护阶段，停机期间发生的拨测故障（该故障只出现在例行维护场景下）", "否"},
            {"间接性故障", "依赖业务故障", "业务的上游故障出现故障而导致本业务牵连影响", "否"},
            {"间接性故障", "专线网络故障", "专线中断导致的故障", "否"},
            {"间接性故障", "运营商公网故障", "运营商公网线路问题导致的故障", "否"},
            {"间接性故障", "私有云基础设施故障", "业务运行在私有云上，私有云故障引发的业务故障", "否"},
            {"间接性故障", "公有云服务故障", "公有云提供的服务本身故障（如数据库实例故障）", "否"},
            {"间接性故障", "监控系统故障", "监控系统异常引起的误报", "否"},
            {"间接性故障", "监控类故障", "服务或接口下线，监控未及时关停", "否"},
            {"间接性故障", "其它故障", "其它非自身原因产生的故障", "否"},
            {"客户类故障", "客户基础设施故障", "客户维护的基础设施发生故障", "否"},
            {"客户类故障", "客户基础设施变更", "基础设施变更导致拨测失效的故障", "否"},
            {"客户类故障", "客户安全误拦截", "因客户安全策略导致服务不可用或其他导致拨测被拦截产生的故障", "否"},
            {"客户类故障", "监控系统故障", "非业务系统故障，因监控系统的故障导致的异常", "否"}
    };

    private final SlaSchemaMapper slaSchemaMapper;
    private final MonitorEditionBackfillService monitorEditionBackfillService;

    public SlaSchemaInitializer(SlaSchemaMapper slaSchemaMapper,
                                MonitorEditionBackfillService monitorEditionBackfillService) {
        this.slaSchemaMapper = slaSchemaMapper;
        this.monitorEditionBackfillService = monitorEditionBackfillService;
    }

    @Override
    public void run(ApplicationArguments args) {
        slaSchemaMapper.createFaultCategoryTable();
        slaSchemaMapper.createReviewEventTable();
        slaSchemaMapper.createSystemOwnerTable();
        slaSchemaMapper.createSliSnapshotTable();
        migrateReviewEventTable();
        migrateSystemOwnerTable();
        for (int i = 0; i < DICT.length; i++) {
            SlaFaultCategory row = new SlaFaultCategory();
            row.setFaultCategory(DICT[i][0]);
            row.setFaultSubcategory(DICT[i][1]);
            row.setFaultDesc(DICT[i][2]);
            row.setCountSla(DICT[i][3]);
            row.setSortOrder(Integer.valueOf(i + 1));
            slaSchemaMapper.insertIgnoreFaultCategory(row);
        }
        seedSystemOwners();
        monitorEditionBackfillService.backfill();
        log.info("MyBatis 建表完成：sla_fault_category={} 行，sla_review_event={} 行，sla_system_owner={} 行",
                slaSchemaMapper.countFaultCategory(),
                slaSchemaMapper.countReviewEvent(),
                slaSchemaMapper.countSystemOwner());
    }

    private void migrateReviewEventTable() {
        if (!hasReviewEventColumn("resolution_process")) {
            slaSchemaMapper.addResolutionProcessColumn();
        }
        if (!hasReviewEventColumn("root_cause")) {
            slaSchemaMapper.addRootCauseColumn();
        }
        if (!hasReviewEventColumn("improvement")) {
            slaSchemaMapper.addImprovementColumn();
        }
        if (!hasReviewEventColumn("exclude_time")) {
            slaSchemaMapper.addExcludeTimeColumn();
        }
        if (hasReviewEventColumn("remark")) {
            slaSchemaMapper.dropRemarkColumn();
        }
    }

    private boolean hasReviewEventColumn(String columnName) {
        return slaSchemaMapper.countReviewEventColumn(columnName) > 0;
    }

    private void migrateSystemOwnerTable() {
        if (slaSchemaMapper.countSystemOwnerColumn("monitor_edition") == 0) {
            slaSchemaMapper.addMonitorEditionColumn();
        }
        if (slaSchemaMapper.countSystemOwnerColumn("write_jdbc_key") == 0) {
            slaSchemaMapper.addWriteJdbcKeyColumn();
        }
    }

    private void seedSystemOwners() {
        try {
            InputStream in = openWorkbook();
            if (in == null) {
                log.warn("未找到 responsible_persons.xlsx，跳过 sla_system_owner 种子");
                return;
            }
            try {
                List<SlaSystemOwner> rows = SystemOwnerWorkbookLoader.load(in);
                int n = 0;
                for (SlaSystemOwner row : rows) {
                    n += slaSchemaMapper.insertIgnoreSystemOwner(row);
                }
                log.info("sla_system_owner 种子：工作簿 {} 行，本次 INSERT IGNORE 影响 {} 行",
                        Integer.valueOf(rows.size()), Integer.valueOf(n));
            } finally {
                in.close();
            }
        } catch (Exception e) {
            log.error("导入 sla_system_owner 种子失败", e);
        }
    }

    private InputStream openWorkbook() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/responsible_persons.xlsx");
        if (resource.exists()) {
            return resource.getInputStream();
        }
        File file = new File("doc/responsible_persons.xlsx");
        if (file.isFile()) {
            return new FileInputStream(file);
        }
        return null;
    }
}
