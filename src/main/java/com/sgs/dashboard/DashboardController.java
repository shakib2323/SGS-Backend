package com.sgs.dashboard;

import com.sgs.bank.BankEntryRepository;
import com.sgs.client.ClientRepository;
import com.sgs.gallery.GalleryRepository;
import com.sgs.inquiry.InquiryRepository;
import com.sgs.inquiry.InquiryStatus;
import com.sgs.offering.ServiceItemRepository;
import com.sgs.project.ProjectRepository;
import com.sgs.tool.ToolRepository;
import com.sgs.user.Role;
import com.sgs.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardController {

    private final GalleryRepository gallery;
    private final ToolRepository tools;
    private final ServiceItemRepository services;
    private final ProjectRepository projects;
    private final ClientRepository clients;
    private final UserRepository users;
    private final InquiryRepository inquiries;
    private final BankEntryRepository bank;

    public record Stats(long galleryImages, long tools, long services, long projects, long clients,
                        long employees, long newInquiries, long bankEntries) {}

    @GetMapping
    @Transactional(readOnly = true)
    public Stats stats() {
        return new Stats(gallery.count(), tools.count(), services.count(), projects.count(), clients.count(),
                users.countByRole(Role.EMPLOYEE), inquiries.countByStatus(InquiryStatus.NEW), bank.count());
    }
}
