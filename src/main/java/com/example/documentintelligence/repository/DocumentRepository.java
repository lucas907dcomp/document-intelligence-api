package com.example.documentintelligence.repository;

import com.example.documentintelligence.domain.Document;
import com.example.documentintelligence.domain.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.status = :status WHERE d.id = :id")
    int updateStatusById(@Param("id") UUID id, @Param("status") DocumentStatus status);
}
