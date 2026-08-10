package com.notificationengine.notificationservice.service;

import com.notificationengine.common.model.Template;
import com.notificationengine.common.repo.TemplateRepository;
import com.notificationengine.notificationservice.models.dtos.TemplateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;

    @Transactional
    public Template createTemplate(TemplateRequest request) {
        log.info("Creating new template with name: {}", request.getName());

        if (templateRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists!");
        }

        Template template = new Template();
        template.setName(request.getName());
        template.setContent(request.getContent());
        template.setPlaceholders(request.getPlaceholders());
        template.setTemplatePriority(request.getTemplatePriority());
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<Template> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Template getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found for ID: " + id));
    }

    @Transactional
    public Template updateTemplate(Long id, TemplateRequest request) {
        log.info("Updating template ID: {}", id);

        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found for ID: " + id));

        template.setName(request.getName());
        template.setContent(request.getContent());
        template.setPlaceholders(request.getPlaceholders());
        template.setTemplatePriority(request.getTemplatePriority());
        template.setUpdatedAt(LocalDateTime.now());

        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        log.warn("Deleting template ID: {}", id);
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found for ID: " + id));
        templateRepository.delete(template);
    }
}
