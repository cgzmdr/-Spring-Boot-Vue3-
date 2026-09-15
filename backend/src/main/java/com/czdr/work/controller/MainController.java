package com.czdr.work.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author cz
 */
@RestController
public class MainController {
    @GetMapping
    public String index() {
        return "main";
    }
}
