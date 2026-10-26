package com.sgs.project;

import com.sgs.common.PageDto;
import com.sgs.project.ProjectDtos.ProjectDto;
import com.sgs.project.ProjectDtos.ProjectForm;
import com.sgs.project.ProjectDtos.ProjectUpdateRequest;
import com.sgs.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @GetMapping("/public/projects")
    public PageDto<ProjectDto> publicList(@RequestParam(name = "category", required = false) String category,
                                          @RequestParam(name = "q", required = false) String q,
                                          @RequestParam(name = "status", required = false) ProjectStatus status,
                                          @RequestParam(name = "featured", required = false) Boolean featured,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "12") int size) {
        return service.search(category, q, status, featured, page, size);
    }

    @GetMapping("/public/projects/{id}")
    public ProjectDto publicGet(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PostMapping(value = "/admin/projects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectDto> create(@Valid @ModelAttribute ProjectForm form, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(form, actor));
    }

    @PutMapping("/admin/projects/{id}")
    public ProjectDto update(@PathVariable("id") Long id, @Valid @RequestBody ProjectUpdateRequest r,
                             @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @PutMapping(value = "/admin/projects/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProjectDto replaceImage(@PathVariable("id") Long id, @RequestParam("image") MultipartFile image,
                                   @AuthenticationPrincipal AuthUser actor) {
        return service.replaceImage(id, image, actor);
    }

    @DeleteMapping("/admin/projects/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
