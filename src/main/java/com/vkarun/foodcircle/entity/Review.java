package com.vkarun.foodcircle.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = {
        // The feed and "reviews by user"; id is the tiebreaker for the cursor.
        @Index(name = "idx_reviews_user_created", columnList = "user_id, created_at desc, id desc"),
        // "Reviews of a place"; also covers the place_id foreign key.
        @Index(name = "idx_reviews_place_created", columnList = "place_id, created_at desc, id desc")
})

@Check(constraints = "(food_rating is null or food_rating between 1 and 5)"
        + " and (ambience_rating is null or ambience_rating between 1 and 5)"
        + " and (food_rating is not null or ambience_rating is not null)")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // The field holds the whole User; the column user_id holds that user's id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // No cascade on delete: a place that has reviews cannot be hard-deleted (it is marked closed instead).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    // Both ratings are optional (1 to 5), but at least one must be given.
    @Column(name = "food_rating")
    private Integer foodRating;

    @Column(name = "ambience_rating")
    private Integer ambienceRating;

    @Column(name = "text", length = 2000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Not a column: the dishes live in review_dishes and point back through its review field.
    // @BatchSize: when a list of reviews is shown, the dishes of up to 50 reviews load in one query instead of one query each.
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ReviewDish> dishes = new ArrayList<>();

    // Required by JPA (Hibernate creates objects through it); not for use in our own code.
    protected Review() {
    }

    public Review(User user, Place place, Integer foodRating, Integer ambienceRating, String text) {
        this.user = user;
        this.place = place;
        this.foodRating = foodRating;
        this.ambienceRating = ambienceRating;
        this.text = text;
        this.createdAt = Instant.now();
    }

    public void addDish(String dishName, Boolean liked) {
        dishes.add(new ReviewDish(this, dishName, liked));
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Place getPlace() {
        return place;
    }

    public Integer getFoodRating() {
        return foodRating;
    }

    public Integer getAmbienceRating() {
        return ambienceRating;
    }

    // Not stored: the average of the ratings given, to one decimal (D13). Same rule for any SQL average.
    public BigDecimal getOverallRating() {
        int sum = 0;
        int count = 0;
        if (foodRating != null) {
            sum += foodRating;
            count++;
        }
        if (ambienceRating != null) {
            sum += ambienceRating;
            count++;
        }
        if (count == 0) {
            return null;
        }
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ReviewDish> getDishes() {
        return Collections.unmodifiableList(dishes);
    }
}
