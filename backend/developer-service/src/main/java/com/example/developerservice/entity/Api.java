package com.example.developerservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "apis")
public class Api {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long developerId;

    private String name;

    private String baseUrl;

    private Integer rateLimitPerMinute;

    private LocalDateTime createdAt;

}