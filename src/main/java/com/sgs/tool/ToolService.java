package com.sgs.tool;

import com.sgs.audit.AuditService;
import com.sgs.common.ApiException;
import com.sgs.common.TextUtil;
import com.sgs.security.AuthUser;
import com.sgs.storage.ImageStorageService;
import com.sgs.storage.ImageStorageService.Profile;
import com.sgs.storage.ImageStorageService.Stored;
import com.sgs.tool.ToolDtos.ToolDto;
import com.sgs.tool.ToolDtos.ToolForm;
import com.sgs.tool.ToolDtos.ToolUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolService {

    private final ToolRepository repo;
    private final ImageStorageService storage;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<ToolDto> listPublic() {
        return repo.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ToolDto> listAdmin() {
        return repo.findAllByOrderByDisplayOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public ToolDto create(ToolForm f, AuthUser actor) {
        Stored s = storage.store(f.getImage(), "tools", Profile.PHOTO);
        Tool t = new Tool();
        t.setTitle(TextUtil.clean(f.getTitle()));
        t.setDescription(TextUtil.blankToNull(f.getDescription()));
        t.setToolLink(TextUtil.blankToNull(f.getToolLink()));
        t.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
        t.setActive(f.getActive() == null || f.getActive());
        t.setImagePath(s.path());
        t.setThumbPath(s.thumbPath());
        repo.save(t);
        audit.log(actor, "TOOL_CREATE", "Tool", t.getId(), t.getTitle());
        return toDto(t);
    }

    @Transactional
    public ToolDto update(Long id, ToolUpdateRequest r, AuthUser actor) {
        Tool t = find(id);
        t.setTitle(TextUtil.clean(r.title()));
        t.setDescription(TextUtil.blankToNull(r.description()));
        t.setToolLink(TextUtil.blankToNull(r.toolLink()));
        if (r.displayOrder() != null) t.setDisplayOrder(r.displayOrder());
        if (r.active() != null) t.setActive(r.active());
        audit.log(actor, "TOOL_UPDATE", "Tool", id, t.getTitle());
        return toDto(t);
    }

    @Transactional
    public ToolDto replaceImage(Long id, MultipartFile file, AuthUser actor) {
        Tool t = find(id);
        String oldMain = t.getImagePath();
        String oldThumb = t.getThumbPath();
        Stored s = storage.store(file, "tools", Profile.PHOTO);
        t.setImagePath(s.path());
        t.setThumbPath(s.thumbPath());
        storage.deleteAfterCommit(oldMain, oldThumb);
        audit.log(actor, "TOOL_IMAGE_REPLACE", "Tool", id, t.getTitle());
        return toDto(t);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        Tool t = find(id);
        repo.delete(t);
        storage.deleteAfterCommit(t.getImagePath(), t.getThumbPath());
        audit.log(actor, "TOOL_DELETE", "Tool", id, t.getTitle());
    }

    private Tool find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Tool"));
    }

    private ToolDto toDto(Tool t) {
        return new ToolDto(t.getId(), t.getTitle(), t.getDescription(), t.getToolLink(),
                storage.url(t.getImagePath()), storage.url(t.getThumbPath()), t.getDisplayOrder(), t.isActive());
    }
}
