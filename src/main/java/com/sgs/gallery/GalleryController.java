package com.sgs.gallery;

import com.sgs.common.PageDto;
import com.sgs.gallery.GalleryDtos.GalleryDto;
import com.sgs.gallery.GalleryDtos.GalleryForm;
import com.sgs.gallery.GalleryDtos.GalleryUpdateRequest;
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
public class GalleryController {

    private final GalleryService service;

    @GetMapping("/public/gallery")
    public PageDto<GalleryDto> publicList(@RequestParam(name = "category", required = false) String category,
                                          @RequestParam(name = "q", required = false) String q,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "24") int size) {
        return service.search(category, q, page, size, false);
    }

    @GetMapping("/public/gallery/{id}")
    public GalleryDto publicGet(@PathVariable("id") Long id) {
        return service.get(id, false);
    }

    @GetMapping("/admin/gallery")
    public PageDto<GalleryDto> adminList(@RequestParam(name = "category", required = false) String category,
                                         @RequestParam(name = "q", required = false) String q,
                                         @RequestParam(name = "page", defaultValue = "0") int page,
                                         @RequestParam(name = "size", defaultValue = "24") int size) {
        return service.search(category, q, page, size, true);
    }

    /** Admin, or an employee with an active upload permission (checked in the service). */
    @PostMapping(value = "/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GalleryDto> create(@Valid @ModelAttribute GalleryForm form, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(form, actor));
    }

    @PutMapping("/admin/gallery/{id}")
    public GalleryDto update(@PathVariable("id") Long id, @Valid @RequestBody GalleryUpdateRequest r,
                             @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @PutMapping(value = "/admin/gallery/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GalleryDto replaceImage(@PathVariable("id") Long id, @RequestParam("image") MultipartFile image,
                                   @AuthenticationPrincipal AuthUser actor) {
        return service.replaceImage(id, image, actor);
    }

    @DeleteMapping("/admin/gallery/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
