package com.project.server.springboot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomAsset entity - stores user-uploaded custom assets
 */
@Entity
@Table(name = "custom_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomAsset {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private String uploadId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "custom_asset_tags", joinColumns = @JoinColumn(name = "asset_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false)
    private Boolean verified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;
}

