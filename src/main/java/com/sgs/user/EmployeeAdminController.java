package com.sgs.user;

import com.sgs.security.AuthUser;
import com.sgs.user.UserDtos.CreateEmployeeRequest;
import com.sgs.user.UserDtos.EmployeeCreated;
import com.sgs.user.UserDtos.EmployeeDto;
import com.sgs.user.UserDtos.ResetPasswordRequest;
import com.sgs.user.UserDtos.ResetPasswordResult;
import com.sgs.user.UserDtos.SuspendRequest;
import com.sgs.user.UserDtos.UpdateEmployeeRequest;
import com.sgs.user.UserDtos.UploadPermissionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/employees")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EmployeeAdminController {

    private final EmployeeService service;

    @PostMapping
    public ResponseEntity<EmployeeCreated> create(@Valid @RequestBody CreateEmployeeRequest r,
                                                  @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r, actor));
    }

    @GetMapping
    public List<EmployeeDto> list(@RequestParam(name = "q", required = false) String q,
                                  @RequestParam(name = "state", required = false) AccountState state) {
        return service.list(q, state);
    }

    @GetMapping("/{id}")
    public EmployeeDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public EmployeeDto update(@PathVariable("id") Long id, @Valid @RequestBody UpdateEmployeeRequest r,
                              @AuthenticationPrincipal AuthUser actor) {
        return service.update(id, r, actor);
    }

    @PostMapping("/{id}/suspend")
    public EmployeeDto suspend(@PathVariable("id") Long id, @Valid @RequestBody SuspendRequest r,
                               @AuthenticationPrincipal AuthUser actor) {
        return service.suspend(id, r, actor);
    }

    @PostMapping("/{id}/unsuspend")
    public EmployeeDto unsuspend(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        return service.unsuspend(id, actor);
    }

    @PostMapping("/reset-password")
    public ResetPasswordResult resetPassword(@Valid @RequestBody ResetPasswordRequest r,
                                             @AuthenticationPrincipal AuthUser actor) {
        return service.resetPassword(r, actor);
    }

    @PostMapping("/{id}/upload-permission")
    public EmployeeDto grantUpload(@PathVariable("id") Long id, @Valid @RequestBody UploadPermissionRequest r,
                                   @AuthenticationPrincipal AuthUser actor) {
        return service.grantUploadPermission(id, r, actor);
    }

    @DeleteMapping("/{id}/upload-permission")
    public EmployeeDto revokeUpload(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        return service.revokeUploadPermission(id, actor);
    }
}
