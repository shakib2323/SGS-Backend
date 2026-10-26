package com.sgs.storage;

import com.sgs.common.ApiException;
import com.sgs.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores processed images on disk (served by Caddy in production). The DB keeps only relative paths.
 * New files are removed if the surrounding DB transaction rolls back; old files are removed after commit.
 */
@Slf4j
@Service
public class ImageStorageService {

    public enum Profile {
        PHOTO(1600, 480),
        LOGO(600, 240);

        final int maxWidth;
        final int thumbWidth;

        Profile(int maxWidth, int thumbWidth) {
            this.maxWidth = maxWidth;
            this.thumbWidth = thumbWidth;
        }
    }

    public record Stored(String path, String thumbPath) {}

    private final AppProperties props;
    private Path root;

    public ImageStorageService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() throws IOException {
        this.root = Path.of(props.storage().root()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Stored store(MultipartFile file, String folder, Profile profile) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Image file is required");
        }
        if (file.getSize() > props.storage().maxFileBytes()) {
            throw ApiException.badRequest("Image is too large (max " + props.storage().maxFileBytes() / (1024 * 1024) + " MB)");
        }
        ImageProcessor.Processed processed;
        try {
            processed = ImageProcessor.process(file.getBytes(), profile.maxWidth, profile.thumbWidth);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }

        String base = folder + "/" + YearMonth.now() + "/" + UUID.randomUUID();
        String mainRel = base + "." + processed.extension();
        String thumbRel = base + "_t." + processed.extension();
        Path mainFile = resolve(mainRel);
        Path thumbFile = resolve(thumbRel);
        try {
            Files.createDirectories(mainFile.getParent());
            Files.write(mainFile, processed.main());
            Files.write(thumbFile, processed.thumb());
        } catch (IOException e) {
            deleteQuietly(mainFile);
            deleteQuietly(thumbFile);
            log.error("Failed to write image", e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", "Could not save the image");
        }
        removeOnRollback(List.of(mainFile, thumbFile));
        return new Stored(mainRel, thumbRel);
    }

    /** Deletes files once the current transaction commits (or immediately if there is none). */
    public void deleteAfterCommit(String... relativePaths) {
        List<Path> files = new ArrayList<>();
        for (String p : relativePaths) {
            if (p != null && !p.isBlank()) files.add(resolve(p));
        }
        if (files.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    files.forEach(ImageStorageService::deleteQuietly);
                }
            });
        } else {
            files.forEach(ImageStorageService::deleteQuietly);
        }
    }

    public String url(String relativePath) {
        if (relativePath == null) return null;
        String base = props.storage().publicBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + relativePath;
    }

    private void removeOnRollback(List<Path> files) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) files.forEach(ImageStorageService::deleteQuietly);
            }
        });
    }

    private Path resolve(String relative) {
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Illegal path");
        return p;
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Could not delete {}", p, e);
        }
    }
}
