package com.sgs.offering;

import com.sgs.audit.AuditService;
import com.sgs.common.ApiException;
import com.sgs.common.Slugs;
import com.sgs.common.TextUtil;
import com.sgs.config.AppProperties;
import com.sgs.offering.ServiceDtos.ReorderImagesRequest;
import com.sgs.offering.ServiceDtos.ServiceDto;
import com.sgs.offering.ServiceDtos.ServiceImageDto;
import com.sgs.offering.ServiceDtos.ServiceRequest;
import com.sgs.security.AuthUser;
import com.sgs.storage.ImageStorageService;
import com.sgs.storage.ImageStorageService.Profile;
import com.sgs.storage.ImageStorageService.Stored;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceItemService {

    private final ServiceItemRepository repo;
    private final ServiceImageRepository images;
    private final ImageStorageService storage;
    private final AuditService audit;
    private final AppProperties props;

    // ---------- reads ----------

    @Transactional(readOnly = true)
    public List<ServiceDto> listPublic() {
        return toDtos(repo.findByActiveTrueOrderByDisplayOrderAscIdAsc());
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> listAdmin() {
        return toDtos(repo.findAllByOrderByDisplayOrderAscIdAsc());
    }

    @Transactional(readOnly = true)
    public ServiceDto getPublicBySlug(String slug) {
        ServiceItem s = repo.findBySlugAndActiveTrue(slug).orElseThrow(() -> ApiException.notFound("Service"));
        return toDtos(List.of(s)).get(0);
    }

    @Transactional(readOnly = true)
    public ServiceDto getAdmin(Long id) {
        return toDtos(List.of(find(id))).get(0);
    }

    // ---------- writes ----------

    @Transactional
    public ServiceDto create(ServiceRequest r, AuthUser actor) {
        ServiceItem s = new ServiceItem();
        s.setSlug(Slugs.unique(Slugs.slugify(r.title()), repo::existsBySlug));
        apply(s, r);
        repo.save(s);
        audit.log(actor, "SERVICE_CREATE", "ServiceItem", s.getId(), s.getTitle());
        return toDtos(List.of(s)).get(0);
    }

    @Transactional
    public ServiceDto update(Long id, ServiceRequest r, AuthUser actor) {
        ServiceItem s = find(id);
        apply(s, r);                                  // slug stays stable so public links keep working
        audit.log(actor, "SERVICE_UPDATE", "ServiceItem", id, s.getTitle());
        return toDtos(List.of(s)).get(0);
    }

    @Transactional
    public void delete(Long id, AuthUser actor) {
        ServiceItem s = find(id);
        List<ServiceImage> imgs = images.findByServiceIdOrderByDisplayOrderAscIdAsc(id);
        List<String> paths = new ArrayList<>();
        for (ServiceImage i : imgs) {
            paths.add(i.getImagePath());
            paths.add(i.getThumbPath());
        }
        images.deleteAll(imgs);
        repo.delete(s);
        storage.deleteAfterCommit(paths.toArray(new String[0]));
        audit.log(actor, "SERVICE_DELETE", "ServiceItem", id, s.getTitle());
    }

    /** Adds up to the remaining capacity (max 8 images per service in total). */
    @Transactional
    public ServiceDto addImages(Long id, List<MultipartFile> files, AuthUser actor) {
        ServiceItem s = find(id);
        List<MultipartFile> real = files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (real.isEmpty()) throw ApiException.badRequest("Select at least one image");
        int max = props.limits().serviceImages();
        long existing = images.countByServiceId(id);
        if (existing + real.size() > max) {
            throw ApiException.badRequest("A service can have at most " + max + " images (currently " + existing + ")");
        }
        List<ServiceImage> current = images.findByServiceIdOrderByDisplayOrderAscIdAsc(id);
        int order = current.isEmpty() ? 0 : current.get(current.size() - 1).getDisplayOrder() + 1;
        for (MultipartFile f : real) {
            Stored st = storage.store(f, "services", Profile.PHOTO);
            ServiceImage img = new ServiceImage();
            img.setService(s);
            img.setImagePath(st.path());
            img.setThumbPath(st.thumbPath());
            img.setDisplayOrder(order++);
            images.save(img);
        }
        audit.log(actor, "SERVICE_IMAGES_ADD", "ServiceItem", id, real.size() + " image(s) added to " + s.getTitle());
        return toDtos(List.of(s)).get(0);
    }

    @Transactional
    public ServiceDto deleteImage(Long id, Long imageId, AuthUser actor) {
        ServiceItem s = find(id);
        ServiceImage img = images.findByIdAndServiceId(imageId, id).orElseThrow(() -> ApiException.notFound("Image"));
        images.delete(img);
        storage.deleteAfterCommit(img.getImagePath(), img.getThumbPath());
        audit.log(actor, "SERVICE_IMAGE_DELETE", "ServiceItem", id, "image " + imageId + " of " + s.getTitle());
        return toDtos(List.of(s)).get(0);
    }

    @Transactional
    public ServiceDto reorderImages(Long id, ReorderImagesRequest r, AuthUser actor) {
        ServiceItem s = find(id);
        List<ServiceImage> current = images.findByServiceIdOrderByDisplayOrderAscIdAsc(id);
        Set<Long> currentIds = new HashSet<>();
        current.forEach(i -> currentIds.add(i.getId()));
        if (r.imageIds().size() != currentIds.size() || !currentIds.equals(new HashSet<>(r.imageIds()))) {
            throw ApiException.badRequest("imageIds must contain every image of this service exactly once");
        }
        Map<Long, ServiceImage> byId = new LinkedHashMap<>();
        current.forEach(i -> byId.put(i.getId(), i));
        int order = 0;
        for (Long imageId : r.imageIds()) byId.get(imageId).setDisplayOrder(order++);
        audit.log(actor, "SERVICE_IMAGES_REORDER", "ServiceItem", id, s.getTitle());
        return toDtos(List.of(s)).get(0);
    }

    // ---------- helpers ----------

    private ServiceItem find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Service"));
    }

    private void apply(ServiceItem s, ServiceRequest r) {
        s.setTitle(TextUtil.clean(r.title()));
        s.setShortDescription(TextUtil.blankToNull(r.shortDescription()));
        s.setDescription(TextUtil.blankToNull(r.description()));
        s.setIconClass(TextUtil.blankToNull(r.iconClass()));
        s.setWarranty(TextUtil.blankToNull(r.warranty()));
        s.setPriceNote(TextUtil.blankToNull(r.priceNote()));
        s.setFeatured(r.featured() != null && r.featured());
        s.setActive(r.active() == null || r.active());
        s.setDisplayOrder(r.displayOrder() == null ? 0 : r.displayOrder());
        s.setMetaTitle(TextUtil.blankToNull(r.metaTitle()));
        s.setMetaDescription(TextUtil.blankToNull(r.metaDescription()));
        s.getFeatures().clear();
        if (r.features() != null) {
            for (String f : r.features()) {
                String t = TextUtil.blankToNull(f);
                if (t != null) s.getFeatures().add(t);
            }
        }
    }

    private List<ServiceDto> toDtos(List<ServiceItem> items) {
        if (items.isEmpty()) return List.of();
        List<Long> ids = items.stream().map(ServiceItem::getId).toList();
        Map<Long, List<ServiceImageDto>> byService = new LinkedHashMap<>();
        for (ServiceImage i : images.findByServiceIdInOrderByDisplayOrderAscIdAsc(ids)) {
            byService.computeIfAbsent(i.getService().getId(), k -> new ArrayList<>())
                    .add(new ServiceImageDto(i.getId(), storage.url(i.getImagePath()), storage.url(i.getThumbPath()), i.getDisplayOrder()));
        }
        List<ServiceDto> out = new ArrayList<>();
        for (ServiceItem s : items) {
            out.add(new ServiceDto(s.getId(), s.getTitle(), s.getSlug(), s.getShortDescription(), s.getDescription(),
                    s.getIconClass(), s.getWarranty(), s.getPriceNote(), s.isFeatured(), s.isActive(),
                    s.getDisplayOrder(), s.getMetaTitle(), s.getMetaDescription(), List.copyOf(s.getFeatures()),
                    byService.getOrDefault(s.getId(), List.of())));
        }
        return out;
    }
}
