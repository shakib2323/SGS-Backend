package com.sgs.client;

import com.sgs.audit.AuditService;
import com.sgs.client.ClientDtos.ClientDto;
import com.sgs.client.ClientDtos.ClientForm;
import com.sgs.client.ClientDtos.ClientUpdateRequest;
import com.sgs.common.ApiException;
import com.sgs.common.TextUtil;
import com.sgs.security.AuthUser;
import com.sgs.storage.ImageStorageService;
import com.sgs.storage.ImageStorageService.Profile;
import com.sgs.storage.ImageStorageService.Stored;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository repo;
    private final ImageStorageService storage;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<ClientDto> listPublic() {
        return repo.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ClientDto> listAdmin() {
        return repo.findAllByOrderByDisplayOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public ClientDto create(ClientForm f, AuthUser actor) {
        Stored s = storage.store(f.getLogo(), "clients", Profile.LOGO);
        Client c = new Client();
        c.setName(TextUtil.clean(f.getName()));
        c.setWebsiteUrl(TextUtil.blankToNull(f.getWebsiteUrl()));
        c.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
        c.setActive(f.getActive() == null || f.getActive());
        c.setLogoPath(s.path());
        c.setThumbPath(s.thumbPath());
        repo.save(c);
        audit.log(actor, "CLIENT_CREATE", "Client", c.getId(), c.getName());
        return toDto(c);
    }

    @Transactional
    public ClientDto update(Long id, ClientUpdateRequest r, AuthUser actor) {
        Client c = find(id);
        c.setName(TextUtil.clean(r.name()));
        c.setWebsiteUrl(TextUtil.blankToNull(r.websiteUrl()));
        if (r.displayOrder() != null) c.setDisplayOrder(r.displayOrder());
        if (r.active() != null) c.setActive(r.active());
        audit.log(actor, "CLIENT_UPDATE", "Client", id, c.getName());
        return toDto(c);
    }

    @Transactional
    public ClientDto replaceLogo(Long id, MultipartFile file, AuthUser actor) {
        Client c = find(id);
        String oldMain = c.getLogoPath();
        String oldThumb = c.getThumbPath();
        Stored s = storage.store(file, "clients", Profile.LOGO);
        c.setLogoPath(s.path());
        c.setThumbPath(s.thumbPath());
        storage.deleteAfterCommit(oldMain, oldThumb);
        audit.log(actor, "CLIENT_LOGO_REPLACE", "Client", id, c.getName());
        return toDto(c);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        Client c = find(id);
        repo.delete(c);
        storage.deleteAfterCommit(c.getLogoPath(), c.getThumbPath());
        audit.log(actor, "CLIENT_DELETE", "Client", id, c.getName());
    }

    private Client find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Client"));
    }

    private ClientDto toDto(Client c) {
        return new ClientDto(c.getId(), c.getName(), storage.url(c.getLogoPath()), c.getWebsiteUrl(),
                c.getDisplayOrder(), c.isActive());
    }
}
