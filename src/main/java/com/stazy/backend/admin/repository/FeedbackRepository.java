package com.stazy.backend.admin.repository;

import com.stazy.backend.admin.entity.Feedback;
import com.stazy.backend.common.enums.FeedbackScope;
import com.stazy.backend.listing.entity.Listing;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByIsPublishedTrueOrderByCreatedAtDesc();

    List<Feedback> findByIsAuthenticatedOrderByCreatedAtDesc(boolean isAuthenticated);

    List<Feedback> findByIsPublishedTrueAndFeedbackScopeOrderByCreatedAtDesc(FeedbackScope feedbackScope);

    List<Feedback> findByFeedbackScopeAndLocationSnapshotOrderByCreatedAtDesc(FeedbackScope feedbackScope, String locationSnapshot);

    List<Feedback> findByFeedbackScopeAndLocationSnapshotAndIsPublishedTrueOrderByCreatedAtDesc(FeedbackScope feedbackScope, String locationSnapshot);

    List<Feedback> findByIsAuthenticatedAndFeedbackScopeOrderByCreatedAtDesc(boolean isAuthenticated, FeedbackScope feedbackScope);

    List<Feedback> findByListingAndIsPublishedTrueOrderByCreatedAtDesc(Listing listing);
    
    List<Feedback> findByUserAndListing(com.stazy.backend.user.entity.User user, Listing listing);
}
