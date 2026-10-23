package com.sla.notifier.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReviewPageController {

    @GetMapping({"/review", "/review/"})
    public String reviewPage() {
        return "redirect:/review/index.html";
    }
}
