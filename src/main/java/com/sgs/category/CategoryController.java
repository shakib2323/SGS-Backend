package com.sgs.category;

import com.sgs.category.CategoryDtos.CategoryDto;
import com.sgs.category.CategoryDtos.CategoryRequest;
import com.sgs.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping("/public/categories")
    public List<CategoryDto> publicList() {
        return service.listPublic();
    }

    @GetMapping("/admin/categories")
    public List<CategoryDto> adminList() {
        return service.listAdmin();
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequest r, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r, actor));
    }

    @PutMapping("/admin/categories/{id}")
    public CategoryDto update(@PathVariable("id") Long id, @Valid @RequestBody CategoryRequest r,
                              @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
