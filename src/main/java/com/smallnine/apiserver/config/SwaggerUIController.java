package com.smallnine.apiserver.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class SwaggerUIController {

    /**
     * 新增路徑 /swagger 到 Swagger UI
     */
    @GetMapping("/swagger")
    public RedirectView swagger() {
        return new RedirectView("/swagger-ui/index.html");
    }

    /**
     * 新增路徑 /api-docs 到 Swagger UI
     */
    @GetMapping("/api-docs")
    public RedirectView apiDocs() {
        return new RedirectView("/swagger-ui/index.html");
    }

    /**
     * 新增路徑 /docs 到 Swagger UI
     */
    @GetMapping("/docs")
    public RedirectView docs() {
        return new RedirectView("/swagger-ui/index.html");
    }

    /**
     * 新增路徑 /api/docs 到 Swagger UI
     */
    @GetMapping("/api/docs")
    public RedirectView apiDocsPath() {
        return new RedirectView("/swagger-ui/index.html");
    }
}