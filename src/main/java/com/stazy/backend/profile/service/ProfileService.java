package com.stazy.backend.profile.service;

import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.profile.dto.OwnerProfileResponse;
import com.stazy.backend.profile.dto.StudentProfileResponse;
import com.stazy.backend.profile.dto.UpdateOwnerProfileRequest;
import com.stazy.backend.profile.dto.UpdateStudentProfileRequest;
import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.profile.repository.OwnerProfileRepository;
import com.stazy.backend.profile.repository.StudentProfileRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.CurrentUserService;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final ProfileCompletionService profileCompletionService;

    public ProfileService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            OwnerProfileRepository ownerProfileRepository,
            ProfileCompletionService profileCompletionService
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.profileCompletionService = profileCompletionService;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentProfile(java.util.UUID userId) {
        User user = requireRole(userId, RoleName.STUDENT);
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    StudentProfile created = new StudentProfile();
                    created.setUser(user);
                    return studentProfileRepository.save(created);
                });
        return mapStudent(user, profile);
    }

    @Transactional
    public StudentProfileResponse updateStudentProfile(java.util.UUID userId, UpdateStudentProfileRequest request) {
        User user = requireRole(userId, RoleName.STUDENT);
        ensureUniqueEmail(request.email(), user);
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    StudentProfile created = new StudentProfile();
                    created.setUser(user);
                    return created;
                });

        user.setDisplayName(request.displayName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setMobileNumber(blankToNull(request.mobileNumber()));
        user.setProfilePhotoUrl(blankToNull(request.profilePhotoUrl()));
        user.setProfilePhotoPublicId(blankToNull(request.profilePhotoPublicId()));

        profile.setCollegeName(blankToNull(request.collegeName()));
        profile.setPrn(blankToNull(request.prn()));
        profile.setEnrollmentNumber(blankToNull(request.enrollmentNumber()));
        profile.setCurrentLocation(blankToNull(request.currentLocation()));

        profileCompletionService.refreshStudentCompletion(user, profile);
        userRepository.save(user);
        studentProfileRepository.save(profile);
        return mapStudent(user, profile);
    }

    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile(java.util.UUID userId) {
        User user = requireRole(userId, RoleName.OWNER);
        OwnerProfile profile = ownerProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    OwnerProfile created = new OwnerProfile();
                    created.setUser(user);
                    return ownerProfileRepository.save(created);
                });
        return mapOwner(user, profile);
    }

    @Transactional
    public OwnerProfileResponse updateOwnerProfile(java.util.UUID userId, UpdateOwnerProfileRequest request) {
        User user = requireRole(userId, RoleName.OWNER);
        ensureUniqueEmail(request.email(), user);
        OwnerProfile profile = ownerProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    OwnerProfile created = new OwnerProfile();
                    created.setUser(user);
                    return created;
                });

        user.setDisplayName(request.displayName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setMobileNumber(blankToNull(request.mobileNumber()));
        user.setProfilePhotoUrl(blankToNull(request.profilePhotoUrl()));
        user.setProfilePhotoPublicId(blankToNull(request.profilePhotoPublicId()));

        profile.setPanNumber(blankToNull(request.panNumber()));
        profile.setPgName(blankToNull(request.pgName()));
        profile.setBusinessName(blankToNull(request.businessName()));
        profile.setAddressLineOne(blankToNull(request.addressLineOne()));
        profile.setAddressLineTwo(blankToNull(request.addressLineTwo()));
        profile.setLocality(blankToNull(request.locality()));
        profile.setPincode(blankToNull(request.pincode()));

        profileCompletionService.refreshOwnerCompletion(user, profile);
        userRepository.save(user);
        ownerProfileRepository.save(profile);
        return mapOwner(user, profile);
    }

    private User requireRole(java.util.UUID userId, RoleName roleName) {
        User user = currentUserService.requireUser(userId);
        if (user.getPrimaryRoleCode() != roleName) {
            throw new BadRequestException("This endpoint is only available for " + roleName.name() + " accounts.");
        }
        return user;
    }

    private void ensureUniqueEmail(String email, User currentUser) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(found -> !found.getId().equals(currentUser.getId()))
                .ifPresent(found -> {
                    throw new BadRequestException("Another account is already using this email.");
                });
    }

    private StudentProfileResponse mapStudent(User user, StudentProfile profile) {
        return new StudentProfileResponse(
                user.getUserCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.getMobileNumber(),
                profile.getCollegeName(),
                profile.getPrn(),
                profile.getEnrollmentNumber(),
                profile.getCurrentLocation(),
                user.getProfilePhotoUrl(),
                user.getCompletionPercentage() == null ? 0 : user.getCompletionPercentage(),
                user.isProfileComplete(),
                user.isIdentityVerified()
        );
    }

    private OwnerProfileResponse mapOwner(User user, OwnerProfile profile) {
        return new OwnerProfileResponse(
                user.getUserCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.getMobileNumber(),
                profile.getPanNumber(),
                profile.getPgName(),
                profile.getBusinessName(),
                profile.getAddressLineOne(),
                profile.getAddressLineTwo(),
                profile.getLocality(),
                profile.getPincode(),
                user.getProfilePhotoUrl(),
                user.getCompletionPercentage() == null ? 0 : user.getCompletionPercentage(),
                user.isProfileComplete(),
                user.isIdentityVerified()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
