package com.sla.notifier.web;

import com.sla.notifier.model.ReviewOverview;
import com.sla.notifier.service.ReviewOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sla")
public class ReviewOverviewController {

    private final ReviewOverviewService service;

    public ReviewOverviewController(ReviewOverviewService service) {
        this.service = service;
    }

    @GetMapping("/review-overview")
    public ReviewOverview reviewOverview() {
        return service.getOverview();
    }
}
