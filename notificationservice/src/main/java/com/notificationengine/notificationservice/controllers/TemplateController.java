package com.notificationengine.notificationservice.controllers;

import com.notificationengine.common.model.Template;
import com.notificationengine.notificationservice.dto.response.APIResponse;
import com.notificationengine.notificationservice.dto.request.TemplateRequest;
import com.notificationengine.notificationservice.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<APIResponse<Template>> createTemplate(@RequestBody TemplateRequest request) {
        Template createdTemplate = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(201, "Template created successfully", createdTemplate));
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<Template>>> getAllTemplates() {
        List<Template> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(
                APIResponse.success(200, "Templates fetched successfully", templates)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<Template>> getTemplateById(@PathVariable Long id) {
        Template template = templateService.getTemplateById(id);
        return ResponseEntity.ok(
                APIResponse.success(200, "Template details retrieved successfully", template)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<Template>> updateTemplate(
            @PathVariable Long id,
            @RequestBody TemplateRequest request) {
        Template updatedTemplate = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(
                APIResponse.success(200, "Template updated successfully", updatedTemplate)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<String>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(
                APIResponse.success(200, "Template deleted successfully", "DELETED")
        );
    }
}