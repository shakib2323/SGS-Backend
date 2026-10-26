package com.sgs.contact;

import com.sgs.audit.AuditService;
import com.sgs.common.ApiException;
import com.sgs.common.TextUtil;
import com.sgs.common.UrlRules;
import com.sgs.contact.ContactDtos.ContactDto;
import com.sgs.contact.ContactDtos.ContactRequest;
import com.sgs.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContactService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{7,25}$");

    private final CompanyContactRepository repo;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<ContactDto> listPublic() {
        return repo.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(ContactService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ContactDto> listAdmin() {
        return repo.findAllByOrderByTypeAscDisplayOrderAscIdAsc().stream().map(ContactService::toDto).toList();
    }

    @Transactional
    public ContactDto create(ContactRequest r, AuthUser actor) {
        CompanyContact c = new CompanyContact();
        apply(c, r);
        repo.save(c);
        audit.log(actor, "CONTACT_CREATE", "CompanyContact", c.getId(), c.getType() + ": " + c.getValue());
        return toDto(c);
    }

    @Transactional
    public ContactDto update(Long id, ContactRequest r, AuthUser actor) {
        CompanyContact c = repo.findById(id).orElseThrow(() -> ApiException.notFound("Contact"));
        apply(c, r);
        audit.log(actor, "CONTACT_UPDATE", "CompanyContact", id, c.getType() + ": " + c.getValue());
        return toDto(c);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        CompanyContact c = repo.findById(id).orElseThrow(() -> ApiException.notFound("Contact"));
        repo.delete(c);
        audit.log(actor, "CONTACT_DELETE", "CompanyContact", id, c.getType() + ": " + c.getValue());
    }

    private void apply(CompanyContact c, ContactRequest r) {
        String value = TextUtil.clean(r.value());
        switch (r.type()) {
            case EMAIL -> {
                if (!EMAIL.matcher(value).matches()) throw ApiException.badRequest("Invalid email address");
            }
            case PHONE, WHATSAPP, SMS -> {
                if (!PHONE.matcher(value).matches()) throw ApiException.badRequest("Invalid phone number");
            }
            case MAP_URL -> {
                if (!UrlRules.isSafeHttpUrl(value, 500) || value.isBlank()) throw ApiException.badRequest("Map link must be an http(s) URL");
            }
            default -> { /* ADDRESS, TELEGRAM, WORKING_HOURS: free text */ }
        }
        c.setType(r.type());
        c.setLabel(TextUtil.blankToNull(r.label()));
        c.setValue(value);
        c.setDisplayOrder(r.displayOrder() == null ? 0 : r.displayOrder());
        c.setActive(r.active() == null || r.active());
    }

    private static ContactDto toDto(CompanyContact c) {
        return new ContactDto(c.getId(), c.getType(), c.getLabel(), c.getValue(), c.getDisplayOrder(), c.isActive());
    }
}
