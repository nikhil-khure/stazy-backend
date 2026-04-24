package com.stazy.backend.profile.service;

import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class ProfileCompletionService {

    public void refreshStudentCompletion(User user, StudentProfile profile) {
        int filled = 0;
        int total = 8; // Increased from 7 to 8 to include gender
        filled += hasValue(user.getDisplayName()) ? 1 : 0;
        filled += hasValue(user.getEmail()) ? 1 : 0;
        filled += hasValue(user.getMobileNumber()) ? 1 : 0;
        filled += hasValue(profile.getCollegeName()) ? 1 : 0;
        filled += hasValue(profile.getEnrollmentNumber()) || hasValue(profile.getPrn()) ? 1 : 0;
        filled += hasValue(profile.getCurrentLocation()) ? 1 : 0;
        filled += hasValue(user.getProfilePhotoUrl()) ? 1 : 0;
        filled += profile.getGender() != null ? 1 : 0; // Added gender check
        updateUserCompletion(user, filled, total, user.isIdentityVerified());
    }

    public void refreshOwnerCompletion(User user, OwnerProfile profile) {
        int filled = 0;
        int total = 8;
        filled += hasValue(user.getDisplayName()) ? 1 : 0;
        filled += hasValue(user.getEmail()) ? 1 : 0;
        filled += hasValue(user.getMobileNumber()) ? 1 : 0;
        filled += hasValue(profile.getPanNumber()) ? 1 : 0;
        filled += hasValue(profile.getPgName()) ? 1 : 0;
        filled += profile.getCity() != null ? 1 : 0;
        filled += hasValue(profile.getAddressLineOne()) ? 1 : 0;
        filled += hasValue(user.getProfilePhotoUrl()) ? 1 : 0;
        updateUserCompletion(user, filled, total, user.isIdentityVerified());
    }

    private void updateUserCompletion(User user, int filled, int total, boolean identityVerified) {
        boolean profileDataComplete = filled >= total;
        int completion = profileDataComplete ? (identityVerified ? 100 : 90) : Math.min(80, Math.round((filled * 80.0f) / total));
        user.setCompletionPercentage(completion);
        user.setProfileComplete(profileDataComplete);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
