package com.sgs.inquiry;

import com.sgs.common.ClientIp;
import com.sgs.common.PageDto;
import com.sgs.config.AppProperties;
import com.sgs.inquiry.InquiryDtos.InquiryDto;
import com.sgs.inquiry.InquiryDtos.InquiryRequest;
import com.sgs.inquiry.InquiryDtos.StatusRequest;
import com.sgs.inquiry.InquiryDtos.SubmitResult;
import com.sgs.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService service;
    private final AppProperties props;

    @PostMapping("/public/inquiries")
    public ResponseEntity<SubmitResult> submit(@Valid @RequestBody InquiryRequest r, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.submit(r, ClientIp.of(req, props.trustProxyHeaders())));
    }

    @GetMapping("/admin/inquiries")
    public PageDto<InquiryDto> list(@RequestParam(name = "status", required = false) InquiryStatus status,
                                    @RequestParam(name = "page", defaultValue = "0") int page,
                                    @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @PatchMapping("/admin/inquiries/{id}/status")
    public InquiryDto setStatus(@PathVariable("id") Long id, @Valid @RequestBody StatusRequest r,
                                @AuthenticationPrincipal AuthUser actor) {
        return service.setStatus(id, r.status(), actor);
    }

    @DeleteMapping("/admin/inquiries/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @AuthenticationPrincipal AuthUser actor) {
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
