package com.sgs.category;

import com.sgs.audit.AuditService;
import com.sgs.category.CategoryDtos.CategoryDto;
import com.sgs.category.CategoryDtos.CategoryRequest;
import com.sgs.common.ApiException;
import com.sgs.common.Slugs;
import com.sgs.common.TextUtil;
import com.sgs.gallery.GalleryRepository;
import com.sgs.project.ProjectRepository;
import com.sgs.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repo;
    private final GalleryRepository gallery;
    private final ProjectRepository projects;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<CategoryDto> listPublic() {
        return repo.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(CategoryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listAdmin() {
        return repo.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest r, AuthUser actor) {
        String name = TextUtil.clean(r.name());
        if (repo.existsByNameIgnoreCase(name)) throw ApiException.conflict("Category already exists");
        Category c = new Category();
        c.setName(name);
        c.setSlug(Slugs.unique(Slugs.slugify(name), repo::existsBySlug));
        c.setDisplayOrder(r.displayOrder() == null ? 0 : r.displayOrder());
        c.setActive(r.active() == null || r.active());
        repo.save(c);
        audit.log(actor, "CATEGORY_CREATE", "Category", c.getId(), name);
        return CategoryDto.from(c);
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest r, AuthUser actor) {
        Category c = repo.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
        String name = TextUtil.clean(r.name());
        if (!c.getName().equalsIgnoreCase(name) && repo.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Category already exists");
        }
        c.setName(name);                    // slug stays stable so existing links keep working
        if (r.displayOrder() != null) c.setDisplayOrder(r.displayOrder());
        if (r.active() != null) c.setActive(r.active());
        audit.log(actor, "CATEGORY_UPDATE", "Category", id, name);
        return CategoryDto.from(c);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        Category c = repo.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
        if (gallery.existsByCategoryId(id) || projects.existsByCategoryId(id)) {
            throw ApiException.conflict("Category is in use. Deactivate it instead, or move its items first.");
        }
        repo.delete(c);
        audit.log(actor, "CATEGORY_DELETE", "Category", id, c.getName());
    }
}
