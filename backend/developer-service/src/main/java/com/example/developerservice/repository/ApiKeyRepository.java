package com.example.developerservice.repository;

import com.example.developerservice.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByApiId(Long apiId);

}