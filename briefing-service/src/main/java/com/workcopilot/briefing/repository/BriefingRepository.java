package com.workcopilot.briefing.repository;

import com.workcopilot.briefing.entity.Briefing;
import com.workcopilot.briefing.entity.BriefingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BriefingRepository extends JpaRepository<Briefing, Long> {

    Optional<Briefing> findByUserIdAndBriefingDate(Long userId, LocalDate date);

    List<Briefing> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Briefing> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BriefingStatus status);
}
