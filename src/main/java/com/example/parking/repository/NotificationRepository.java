package com.example.parking.repository;

import com.example.parking.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for admin Notification entities.
 * Every query is scoped to a single recipient so one admin can never read or modify another's rows.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findRecent(@Param("recipientId") Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadStatusFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readStatus = true, n.readAt = :now "
            + "WHERE n.recipient.id = :recipientId AND n.readStatus = false")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);
}
