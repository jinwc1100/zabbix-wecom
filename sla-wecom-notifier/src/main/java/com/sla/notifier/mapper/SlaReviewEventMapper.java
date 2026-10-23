package com.sla.notifier.mapper;

import com.sla.notifier.model.ReviewEventQuery;
import com.sla.notifier.model.OwnerIncompleteCount;
import com.sla.notifier.model.SlaFaultCategory;
import com.sla.notifier.model.SlaReviewEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SlaReviewEventMapper {

    /**
     * @return 1 if inserted, 0 if unique key already exists
     */
    int insertIgnore(SlaReviewEvent event);

    /**
     * @return 1 if a row with the unique key was updated, 0 if none matched
     */
    int updateOwnerByIdentity(SlaReviewEvent event);

    List<SlaReviewEvent> listByStartIdentity(SlaReviewEvent event);

    /**
     * Overwrite down_end on the kept row; when count_sla=否 also set exclude_time.
     */
    int updateAlignedDownEnd(@Param("id") Long id,
                             @Param("newDownEnd") java.time.LocalDateTime newDownEnd);

    int deleteById(@Param("id") Long id);

    List<SlaFaultCategory> listFaultCategories();

    SlaFaultCategory findFaultCategory(@Param("faultCategory") String faultCategory,
                                       @Param("faultSubcategory") String faultSubcategory);

    List<SlaReviewEvent> listReviewEvents(ReviewEventQuery query);

    long countReviewEvents(ReviewEventQuery query);

    SlaReviewEvent findById(@Param("id") Long id);

    List<SlaReviewEvent> listReviewedEvents();

    int updateReviewFields(SlaReviewEvent event);

    int refreshExcludeTime();

    long countIncompleteReviewEvents();

    List<String> listIncompleteOwnerNames();

    long countIncompleteBySystemLevel(@Param("systemLevel") String systemLevel);

    List<OwnerIncompleteCount> countIncompleteByOwner();
}
