package com.knowledgebase.backend.controller;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Map<String, Object> handleError() {
        Map<String, Object> error = new HashMap<>();
        error.put("code", 404);
        error.put("message", "This path does not exist");
        return error;
    }
}
