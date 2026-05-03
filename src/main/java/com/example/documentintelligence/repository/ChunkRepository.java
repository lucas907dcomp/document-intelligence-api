package com.example.documentintelligence.repository;

import com.example.documentintelligence.domain.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    List<Chunk> findByDocumentId(UUID documentId);
}
