package com.hyetaekon.hyetaekon.userInterest.entity;

import com.hyetaekon.hyetaekon.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Table(name = "user_interest", indexes = {
    @Index(name = "idx_interest", columnList = "interest")
})
public class UserInterest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String interest;

    @Override
    public String toString() {
        return "{user_id: " + user.getId() + ", keyword: " +  interest + "}";
    }
}
