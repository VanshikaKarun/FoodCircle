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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// Java fields are camelCase; every column name is written out explicitly (D18).
@Entity
@Table(name = "review_dishes",
        indexes = @Index(name = "idx_review_dishes_review", columnList = "review_id"))
public class ReviewDish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // The field holds the whole Review; the column review_id holds that review's id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Review review;

    // Free text typed by the user (no menu table).
    @Column(name = "dish_name", nullable = false, length = 100)
    private String dishName;

    // Optional: did they like it?
    @Column(name = "liked")
    private Boolean liked;

    // Required by JPA (Hibernate creates objects through it); not for use in our own code.
    protected ReviewDish() {
    }

    ReviewDish(Review review, String dishName, Boolean liked) {
        this.review = review;
        this.dishName = dishName;
        this.liked = liked;
    }

    public Long getId() {
        return id;
    }

    public Review getReview() {
        return review;
    }

    public String getDishName() {
        return dishName;
    }

    public Boolean getLiked() {
        return liked;
    }
}
