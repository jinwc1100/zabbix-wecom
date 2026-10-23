package com.sla.notifier.web;

import com.sla.notifier.model.ReviewEventPage;
import com.sla.notifier.model.ReviewEventUpdateRequest;
import com.sla.notifier.model.SlaFaultCategory;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.service.ReviewEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sla")
public class ReviewEventController {

    private final ReviewEventService service;

    public ReviewEventController(ReviewEventService service) {
        this.service = service;
    }

    @GetMapping("/fault-categories")
    public List<SlaFaultCategory> faultCategories() {
        return service.listFaultCategories();
    }

    @GetMapping("/review-events")
    public ReviewEventPage reviewEvents(
            @RequestParam(value = "system", required = false) String system,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "timeMode", required = false) String timeMode,
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "reviewStatus", required = false) String reviewStatus,
            @RequestParam(value = "systemLevel", required = false) String systemLevel,
            @RequestParam(value = "countSla", required = false) String countSla) {
        return service.list(system, owner, timeMode, month, from, to, page, size,
                reviewStatus, systemLevel, countSla);
    }

    @PutMapping("/review-events/{id}")
    public SlaReviewEvent updateReviewEvent(@PathVariable("id") Long id,
                                            @RequestBody ReviewEventUpdateRequest request) {
        return service.update(id, request);
    }
}
