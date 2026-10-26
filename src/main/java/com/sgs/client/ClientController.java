package com.sgs.client;

import com.sgs.client.ClientDtos.ClientDto;
import com.sgs.client.ClientDtos.ClientForm;
import com.sgs.client.ClientDtos.ClientUpdateRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService service;

    @GetMapping("/public/clients")
    public List<ClientDto> publicList() {
        return service.listPublic();
    }

    @GetMapping("/admin/clients")
    public List<ClientDto> adminList() {
        return service.listAdmin();
    }

    @PostMapping(value = "/admin/clients", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientDto> create(@Valid @ModelAttribute ClientForm form, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(form, actor));
    }

    @PutMapping("/admin/clients/{id}")
    public ClientDto update(@PathVariable("id") Long id, @Valid @RequestBody ClientUpdateRequest r,
                            @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @PutMapping(value = "/admin/clients/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClientDto replaceLogo(@PathVariable("id") Long id, @RequestParam("logo") MultipartFile logo,
                                 @AuthenticationPrincipal AuthUser actor) {
        return service.replaceLogo(id, logo, actor);
    }

    @DeleteMapping("/admin/clients/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
