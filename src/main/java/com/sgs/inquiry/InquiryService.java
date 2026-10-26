package com.sgs.inquiry;

import com.sgs.audit.AuditService;
import com.sgs.common.ApiException;
import com.sgs.common.PageDto;
import com.sgs.common.RateLimiter;
import com.sgs.common.TextUtil;
import com.sgs.config.AppProperties;
import com.sgs.inquiry.InquiryDtos.InquiryDto;
import com.sgs.inquiry.InquiryDtos.InquiryRequest;
import com.sgs.inquiry.InquiryDtos.SubmitResult;
import com.sgs.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository repo;
    private final TurnstileService turnstile;
    private final RateLimiter limiter;
    private final AppProperties props;
    private final AuditService audit;

    private static final SubmitResult OK = new SubmitResult("Thank you! We will contact you soon.");

    @Transactional
    public SubmitResult submit(InquiryRequest r, String ip) {
        // Honeypot: bots fill hidden fields. Pretend success so they do not adapt.
        if (r.website() != null && !r.website().isBlank()) return OK;

        if (!limiter.tryAcquire("inquiry:" + ip, props.limits().inquiriesPerHour(), Duration.ofHours(1))) {
            throw ApiException.tooMany("Too many messages from your network. Please try again later.");
        }
        if ((r.email() == null || r.email().isBlank()) && (r.phone() == null || r.phone().isBlank())) {
            throw ApiException.badRequest("Please provide an email address or a phone number");
        }
        if (!turnstile.verify(r.turnstileToken(), ip)) {
            throw ApiException.badRequest("Captcha verification failed. Please try again.");
        }

        Inquiry i = new Inquiry();
        i.setName(TextUtil.clean(r.name()));
        i.setEmail(TextUtil.blankToNull(r.email()));
        i.setPhone(TextUtil.blankToNull(r.phone()));
        i.setServiceInterest(TextUtil.blankToNull(r.serviceInterest()));
        i.setMessage(TextUtil.clean(r.message()));
        i.setIpAddress(ip);
        repo.save(i);
        return OK;
    }

    @Transactional(readOnly = true)
    public PageDto<InquiryDto> list(InquiryStatus status, int page, int size) {
        Pageable pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Inquiry> p = status == null ? repo.findAll(pr) : repo.findByStatus(status, pr);
        return PageDto.of(p.map(InquiryService::toDto));
    }

    @Transactional
    public InquiryDto setStatus(Long id, InquiryStatus status, AuthUser actor) {
        Inquiry i = repo.findById(id).orElseThrow(() -> ApiException.notFound("Inquiry"));
        i.setStatus(status);
        audit.log(actor, "INQUIRY_STATUS", "Inquiry", id, status.name());
        return toDto(i);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        Inquiry i = repo.findById(id).orElseThrow(() -> ApiException.notFound("Inquiry"));
        repo.delete(i);
        audit.log(actor, "INQUIRY_DELETE", "Inquiry", id, null);
    }

    private static InquiryDto toDto(Inquiry i) {
        return new InquiryDto(i.getId(), i.getName(), i.getEmail(), i.getPhone(), i.getServiceInterest(),
                i.getMessage(), i.getStatus(), i.getCreatedAt());
    }
}
