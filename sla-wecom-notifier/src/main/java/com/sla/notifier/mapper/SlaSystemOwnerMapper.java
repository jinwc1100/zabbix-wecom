package com.sla.notifier.mapper;

import com.sla.notifier.model.SlaSystemOwner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SlaSystemOwnerMapper {

    List<SlaSystemOwner> selectAll();

    SlaSystemOwner selectByLevelAndName(@Param("systemLevel") String systemLevel,
                                        @Param("systemName") String systemName);

    int updateMonitorRouting(@Param("systemLevel") String systemLevel,
                             @Param("systemName") String systemName,
                             @Param("monitorEdition") String monitorEdition,
                             @Param("writeJdbcKey") String writeJdbcKey);

    String selectMobileByOwnerName(@Param("ownerName") String ownerName);
}
