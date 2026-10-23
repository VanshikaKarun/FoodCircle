package com.vkarun.foodcircle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

// Java fields are camelCase; every column name is written out explicitly (D18).
@Entity
@Table(name = "follows",
        // One follow per pair; its index also answers "who do I follow?" (leading column follower_id).
        uniqueConstraints = @UniqueConstraint(name = "uk_follows_pair", columnNames = {"follower_id", "followee_id"}),
        // "Who follows me?"
        indexes = @Index(name = "idx_follows_followee", columnList = "followee_id"))
@Check(constraints = "follower_id <> followee_id")
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // The one who follows. The field holds the whole User; the column follower_id holds that user's id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User follower;

    // The one being followed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User followee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Required by JPA (Hibernate creates objects through it); not for use in our own code.
    protected Follow() {
    }

    public Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getFollower() {
        return follower;
    }

    public User getFollowee() {
        return followee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
