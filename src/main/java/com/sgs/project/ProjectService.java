package com.sgs.project;

import com.sgs.audit.AuditService;
import com.sgs.category.Category;
import com.sgs.category.CategoryDtos.CategoryRef;
import com.sgs.category.CategoryRepository;
import com.sgs.common.ApiException;
import com.sgs.common.PageDto;
import com.sgs.common.TextUtil;
import com.sgs.project.ProjectDtos.ProjectDto;
import com.sgs.project.ProjectDtos.ProjectForm;
import com.sgs.project.ProjectDtos.ProjectUpdateRequest;
import com.sgs.security.AuthUser;
import com.sgs.storage.ImageStorageService;
import com.sgs.storage.ImageStorageService.Profile;
import com.sgs.storage.ImageStorageService.Stored;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository repo;
    private final CategoryRepository categories;
    private final ImageStorageService storage;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageDto<ProjectDto> search(String categorySlug, String q, ProjectStatus status, Boolean featured, int page, int size) {
        Page<Project> p = repo.findAll(spec(categorySlug, q, status, featured), pageable(page, size));
        return PageDto.of(p.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public ProjectDto create(ProjectForm f, AuthUser actor) {
        validateYears(f.getStartedYear(), f.getCompletedYear(), f.getStatus());
        Category c = activeCategory(f.getCategoryId());
        Stored s = storage.store(f.getImage(), "projects", Profile.PHOTO);
        Project p = new Project();
        p.setTitle(TextUtil.clean(f.getTitle()));
        p.setDescription(TextUtil.blankToNull(f.getDescription()));
        p.setCategory(c);
        p.setLocation(TextUtil.blankToNull(f.getLocation()));
        p.setClientName(TextUtil.blankToNull(f.getClientName()));
        p.setStartedYear(f.getStartedYear());
        p.setCompletedYear(f.getCompletedYear());
        p.setStatus(f.getStatus());
        p.setAreaSqFt(f.getAreaSqFt());
        p.setFeatured(f.getFeatured() != null && f.getFeatured());
        p.setImagePath(s.path());
        p.setThumbPath(s.thumbPath());
        repo.save(p);
        audit.log(actor, "PROJECT_CREATE", "Project", p.getId(), p.getTitle());
        return toDto(p);
    }

    @Transactional
    public ProjectDto update(Long id, ProjectUpdateRequest r, AuthUser actor) {
        validateYears(r.startedYear(), r.completedYear(), r.status());
        Project p = find(id);
        p.setTitle(TextUtil.clean(r.title()));
        p.setDescription(TextUtil.blankToNull(r.description()));
        if (!p.getCategory().getId().equals(r.categoryId())) p.setCategory(activeCategory(r.categoryId()));
        p.setLocation(TextUtil.blankToNull(r.location()));
        p.setClientName(TextUtil.blankToNull(r.clientName()));
        p.setStartedYear(r.startedYear());
        p.setCompletedYear(r.status() == ProjectStatus.COMPLETED ? r.completedYear() : null);
        p.setStatus(r.status());
        p.setAreaSqFt(r.areaSqFt());
        p.setFeatured(r.featured() != null && r.featured());
        audit.log(actor, "PROJECT_UPDATE", "Project", id, p.getTitle());
        return toDto(p);
    }

    @Transactional
    public ProjectDto replaceImage(Long id, MultipartFile file, AuthUser actor) {
        Project p = find(id);
        String oldMain = p.getImagePath();
        String oldThumb = p.getThumbPath();
        Stored s = storage.store(file, "projects", Profile.PHOTO);
        p.setImagePath(s.path());
        p.setThumbPath(s.thumbPath());
        storage.deleteAfterCommit(oldMain, oldThumb);
        audit.log(actor, "PROJECT_IMAGE_REPLACE", "Project", id, p.getTitle());
        return toDto(p);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        Project p = find(id);
        repo.delete(p);
        storage.deleteAfterCommit(p.getImagePath(), p.getThumbPath());
        audit.log(actor, "PROJECT_DELETE", "Project", id, p.getTitle());
    }

    // ---------- helpers ----------

    private void validateYears(Integer started, Integer completed, ProjectStatus status) {
        int maxYear = LocalDate.now(clock.withZone(ZoneOffset.UTC)).getYear() + 1;
        if (started > maxYear) throw ApiException.badRequest("Started year cannot be in the far future");
        if (status == ProjectStatus.COMPLETED) {
            if (completed == null) throw ApiException.badRequest("Completed year is required for a completed project");
            if (completed < started) throw ApiException.badRequest("Completed year cannot be before started year");
        }
    }

    private Project find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Project"));
    }

    private Category activeCategory(Long id) {
        return categories.findById(id).filter(Category::isActive)
                .orElseThrow(() -> ApiException.badRequest("Unknown or inactive category"));
    }

    private ProjectDto toDto(Project p) {
        return new ProjectDto(p.getId(), p.getTitle(), p.getDescription(), CategoryRef.from(p.getCategory()),
                p.getLocation(), p.getClientName(), p.getStartedYear(), p.getCompletedYear(), p.getStatus(),
                p.getAreaSqFt(), p.isFeatured(), storage.url(p.getImagePath()), storage.url(p.getThumbPath()),
                p.getCreatedAt());
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 60),
                Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("startedYear"), Sort.Order.desc("id")));
    }

    private static Specification<Project> spec(String categorySlug, String q, ProjectStatus status, Boolean featured) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (categorySlug != null && !categorySlug.isBlank()) {
                ps.add(cb.equal(root.get("category").get("slug"), categorySlug.trim().toLowerCase(Locale.ROOT)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (featured != null) ps.add(cb.equal(root.get("featured"), featured));
            if (q != null && !q.isBlank()) {
                String like = "%" + TextUtil.escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.get("clientName")), like, '\\'),
                        cb.like(cb.lower(root.get("location")), like, '\\')));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
