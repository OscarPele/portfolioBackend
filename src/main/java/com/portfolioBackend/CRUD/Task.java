package com.portfolioBackend.CRUD;

import com.portfolioBackend.auth.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "completed_by_user_id")
    private User completedBy;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId()                   { return id; }
    public User getUser()                 { return user; }
    public void setUser(User user)        { this.user = user; }
    public User getCompletedBy()          { return completedBy; }
    public void setCompletedBy(User user) { this.completedBy = user; }
    public String getTitle()              { return title; }
    public void setTitle(String title)    { this.title = title; }
    public boolean isCompleted()          { return completed; }
    public void setCompleted(boolean c)   { this.completed = c; }
    public Instant getCreatedAt()         { return createdAt; }
    public Instant getUpdatedAt()         { return updatedAt; }
}
