package com.sgs.gallery;

import com.sgs.audit.AuditService;
import com.sgs.category.Category;
import com.sgs.category.CategoryDtos.CategoryRef;
import com.sgs.category.CategoryRepository;
import com.sgs.common.ApiException;
import com.sgs.common.PageDto;
import com.sgs.common.TextUtil;
import com.sgs.gallery.GalleryDtos.GalleryDto;
import com.sgs.gallery.GalleryDtos.GalleryForm;
import com.sgs.gallery.GalleryDtos.GalleryUpdateRequest;
import com.sgs.security.AuthUser;
import com.sgs.storage.ImageStorageService;
import com.sgs.storage.ImageStorageService.Profile;
import com.sgs.storage.ImageStorageService.Stored;
import com.sgs.user.User;
import com.sgs.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryRepository repo;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final ImageStorageService storage;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageDto<GalleryDto> search(String categorySlug, String q, int page, int size, boolean includeUploader) {
        Page<GalleryImage> p = repo.findAll(spec(categorySlug, q), pageable(page, size));
        return PageDto.of(p.map(g -> toDto(g, includeUploader)));
    }

    @Transactional(readOnly = true)
    public GalleryDto get(Long id, boolean includeUploader) {
        return toDto(find(id), includeUploader);
    }

    /** Admins, or employees whose time-limited upload permission is still active. */
    @Transactional
    public GalleryDto create(GalleryForm f, AuthUser actor) {
        User u = users.findById(actor.id()).orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "Account not found"));
        if (!u.canUploadNow(clock.instant())) {
            throw ApiException.forbidden("UPLOAD_NOT_ALLOWED", "You do not have permission to upload images right now");
        }
        Category c = activeCategory(f.getCategoryId());
        Stored s = storage.store(f.getImage(), "gallery", Profile.PHOTO);

        GalleryImage g = new GalleryImage();
        g.setTitle(TextUtil.clean(f.getTitle()));
        g.setDescription(TextUtil.blankToNull(f.getDescription()));
        g.setSocialLink(TextUtil.blankToNull(f.getSocialLink()));
        g.setCategory(c);
        g.setImagePath(s.path());
        g.setThumbPath(s.thumbPath());
        g.setUploadedBy(u);
        repo.save(g);
        audit.log(actor, "GALLERY_CREATE", "GalleryImage", g.getId(), g.getTitle());
        return toDto(g, true);
    }

    @Transactional
    public GalleryDto update(Long id, GalleryUpdateRequest r, AuthUser actor) {
        GalleryImage g = find(id);
        g.setTitle(TextUtil.clean(r.title()));
        g.setDescription(TextUtil.blankToNull(r.description()));
        g.setSocialLink(TextUtil.blankToNull(r.socialLink()));
        if (!g.getCategory().getId().equals(r.categoryId())) g.setCategory(activeCategory(r.categoryId()));
        audit.log(actor, "GALLERY_UPDATE", "GalleryImage", id, g.getTitle());
        return toDto(g, true);
    }

    @Transactional
    public GalleryDto replaceImage(Long id, MultipartFile file, AuthUser actor) {
        GalleryImage g = find(id);
        String oldMain = g.getImagePath();
        String oldThumb = g.getThumbPath();
        Stored s = storage.store(file, "gallery", Profile.PHOTO);
        g.setImagePath(s.path());
        g.setThumbPath(s.thumbPath());
        storage.deleteAfterCommit(oldMain, oldThumb);
        audit.log(actor, "GALLERY_IMAGE_REPLACE", "GalleryImage", id, g.getTitle());
        return toDto(g, true);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        GalleryImage g = find(id);
        String main = g.getImagePath();
        String thumb = g.getThumbPath();
        repo.delete(g);
        storage.deleteAfterCommit(main, thumb);
        audit.log(actor, "GALLERY_DELETE", "GalleryImage", id, g.getTitle());
    }

    // ---------- helpers ----------

    private GalleryImage find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Gallery image"));
    }

    private Category activeCategory(Long id) {
        return categories.findById(id).filter(Category::isActive)
                .orElseThrow(() -> ApiException.badRequest("Unknown or inactive category"));
    }

    private GalleryDto toDto(GalleryImage g, boolean includeUploader) {
        return new GalleryDto(g.getId(), g.getTitle(), g.getDescription(), g.getSocialLink(),
                CategoryRef.from(g.getCategory()), storage.url(g.getImagePath()), storage.url(g.getThumbPath()),
                g.getCreatedAt(),
                includeUploader && g.getUploadedBy() != null ? g.getUploadedBy().getUsername() : null);
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 60),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private static Specification<GalleryImage> spec(String categorySlug, String q) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (categorySlug != null && !categorySlug.isBlank()) {
                ps.add(cb.equal(root.get("category").get("slug"), categorySlug.trim().toLowerCase(Locale.ROOT)));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + TextUtil.escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                ps.add(cb.like(cb.lower(root.get("title")), like, '\\'));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
