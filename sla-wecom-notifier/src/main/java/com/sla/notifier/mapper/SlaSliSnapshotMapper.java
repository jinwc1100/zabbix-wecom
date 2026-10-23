package com.sla.notifier.mapper;

import com.sla.notifier.model.SlaSliSnapshot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SlaSliSnapshotMapper {

    int upsert(SlaSliSnapshot row);
}
