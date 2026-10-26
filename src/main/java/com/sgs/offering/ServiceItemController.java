package com.sgs.offering;

import com.sgs.offering.ServiceDtos.ReorderImagesRequest;
import com.sgs.offering.ServiceDtos.ServiceDto;
import com.sgs.offering.ServiceDtos.ServiceRequest;
import com.sgs.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class ServiceItemController {

    private final ServiceItemService service;

    @GetMapping("/public/services")
    public List<ServiceDto> publicList() {
        return service.listPublic();
    }

    @GetMapping("/public/services/{slug}")
    public ServiceDto publicGet(@PathVariable("slug") String slug) {
        return service.getPublicBySlug(slug);
    }

    @GetMapping("/admin/services")
    public List<ServiceDto> adminList() {
        return service.listAdmin();
    }

    @GetMapping("/admin/services/{id}")
    public ServiceDto adminGet(@PathVariable("id") Long id) {
        return service.getAdmin(id);
    }

    @PostMapping("/admin/services")
    public ResponseEntity<ServiceDto> create(@Valid @RequestBody ServiceRequest r, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r, actor));
    }

    @PutMapping("/admin/services/{id}")
    public ServiceDto update(@PathVariable("id") Long id, @Valid @RequestBody ServiceRequest r,
                             @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @DeleteMapping("/admin/services/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    /** multipart field name: images (repeat it for several files). Max 8 images per service in total. */
    @PostMapping(value = "/admin/services/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServiceDto addImages(@PathVariable("id") Long id, @RequestParam("images") List<MultipartFile> images,
                                @AuthenticationPrincipal AuthUser actor) {
        return service.addImages(id, images, actor);
    }

    @DeleteMapping("/admin/services/{id}/images/{imageId}")
    public ServiceDto deleteImage(@PathVariable("id") Long id, @PathVariable("imageId") Long imageId,
                                  @AuthenticationPrincipal AuthUser actor) {
        return service.deleteImage(id, imageId, actor);
    }

    @PutMapping("/admin/services/{id}/images/order")
    public ServiceDto reorder(@PathVariable("id") Long id, @Valid @RequestBody ReorderImagesRequest r,
                              @AuthenticationPrincipal AuthUser actor) {
        return service.reorderImages(id, r, actor);
    }
}
