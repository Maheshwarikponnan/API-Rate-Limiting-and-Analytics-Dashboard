package com.example.developerservice.repository;

import com.example.developerservice.entity.Developer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeveloperRepository extends JpaRepository<Developer, Long> {

    Developer findByEmail(String email);

}