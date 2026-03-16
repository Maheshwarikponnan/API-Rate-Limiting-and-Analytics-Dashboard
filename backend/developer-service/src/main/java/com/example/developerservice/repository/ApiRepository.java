package com.example.developerservice.repository;

import com.example.developerservice.entity.Api;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiRepository extends JpaRepository<Api, Long> {

    List<Api> findByDeveloperId(Long developerId);

}