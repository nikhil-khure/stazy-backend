package com.stazy.backend.profile.service;

import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class ProfileCompletionService {

    public void refreshStudentCompletion(User user, StudentProfile profile) {
        int filled = 0;
        int total = 7;
        filled += hasValue(user.getDisplayName()) ? 1 : 0;
        filled += hasValue(user.getEmail()) ? 1 : 0;
        filled += hasValue(user.getMobileNumber()) ? 1 : 0;
        filled += hasValue(profile.getCollegeName()) ? 1 : 0;
        filled += hasValue(profile.getEnrollmentNumber()) || hasValue(profile.getPrn()) ? 1 : 0;
        filled += hasValue(profile.getCurrentLocation()) ? 1 : 0;
        filled += hasValue(user.getProfilePhotoUrl()) ? 1 : 0;
        updateUserCompletion(user, filled, total);
    }

    public void refreshOwnerCompletion(User user, OwnerProfile profile) {
        int filled = 0;
        int total = 7;
        filled += hasValue(user.getDisplayName()) ? 1 : 0;
        filled += hasValue(user.getEmail()) ? 1 : 0;
        filled += hasValue(user.getMobileNumber()) ? 1 : 0;
        filled += hasValue(profile.getPanNumber()) ? 1 : 0;
        filled += hasValue(profile.getPgName()) ? 1 : 0;
        filled += hasValue(profile.getAddressLineOne()) || hasValue(profile.getLocality()) ? 1 : 0;
        filled += hasValue(user.getProfilePhotoUrl()) ? 1 : 0;
        updateUserCompletion(user, filled, total);
    }

    private void updateUserCompletion(User user, int filled, int total) {
        int completion = Math.round((filled * 100.0f) / total);
        user.setCompletionPercentage(completion);
        user.setProfileComplete(completion == 100);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
