package com.context_mcp.context_mcp.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tags")
public class TagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255, unique = true)
    private String name;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
