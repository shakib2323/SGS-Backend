package com.sgs.contact;

import com.sgs.contact.ContactDtos.ContactDto;
import com.sgs.contact.ContactDtos.ContactRequest;
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
public class ContactController {

    private final ContactService service;

    @GetMapping("/public/contact")
    public List<ContactDto> publicList() {
        return service.listPublic();
    }

    @GetMapping("/admin/contact")
    public List<ContactDto> adminList() {
        return service.listAdmin();
    }

    @PostMapping("/admin/contact")
    public ResponseEntity<ContactDto> create(@Valid @RequestBody ContactRequest r, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r, actor));
    }

    @PutMapping("/admin/contact/{id}")
    public ContactDto update(@PathVariable("id") Long id, @Valid @RequestBody ContactRequest r,
                             @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @DeleteMapping("/admin/contact/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
