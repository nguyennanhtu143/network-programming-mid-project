package dev.p3ntest.zombies.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "\"CustomAsset\"")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomAsset {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private String uploadId;

    @Column(nullable = false)
    private String name;

    @Column
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


