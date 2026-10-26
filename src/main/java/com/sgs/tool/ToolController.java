package com.sgs.tool;

import com.sgs.security.AuthUser;
import com.sgs.tool.ToolDtos.ToolDto;
import com.sgs.tool.ToolDtos.ToolForm;
import com.sgs.tool.ToolDtos.ToolUpdateRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService service;

    @GetMapping("/public/tools")
    public List<ToolDto> publicList() {
        return service.listPublic();
    }

    @GetMapping("/admin/tools")
    public List<ToolDto> adminList() {
        return service.listAdmin();
    }

    @PostMapping(value = "/admin/tools", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ToolDto> create(@Valid @ModelAttribute ToolForm form, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(form, actor));
    }

    @PutMapping("/admin/tools/{id}")
    public ToolDto update(@PathVariable("id") Long id, @Valid @RequestBody ToolUpdateRequest r,
                          @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @PutMapping(value = "/admin/tools/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ToolDto replaceImage(@PathVariable("id") Long id, @RequestParam("image") MultipartFile image,
                                @AuthenticationPrincipal AuthUser actor) {
        return service.replaceImage(id, image, actor);
    }

    @DeleteMapping("/admin/tools/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
