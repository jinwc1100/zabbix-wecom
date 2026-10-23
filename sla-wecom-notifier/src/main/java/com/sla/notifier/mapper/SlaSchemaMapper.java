package com.sla.notifier.mapper;

import com.sla.notifier.model.SlaFaultCategory;
import com.sla.notifier.model.SlaSystemOwner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SlaSchemaMapper {

    void createFaultCategoryTable();

    void createReviewEventTable();

    void createSystemOwnerTable();

    void createSliSnapshotTable();

    int countReviewEventColumn(@Param("columnName") String columnName);

    int countSystemOwnerColumn(@Param("columnName") String columnName);

    void addMonitorEditionColumn();

    void addWriteJdbcKeyColumn();

    void addResolutionProcessColumn();

    void addRootCauseColumn();

    void addImprovementColumn();

    void addExcludeTimeColumn();

    void dropRemarkColumn();

    int insertIgnoreFaultCategory(SlaFaultCategory row);

    int insertIgnoreSystemOwner(SlaSystemOwner row);

    int countFaultCategory();

    int countSystemOwner();

    int countReviewEvent();
}
