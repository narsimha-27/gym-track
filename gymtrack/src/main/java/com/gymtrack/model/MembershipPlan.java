package com.gymtrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Plan name is required")
    private String name;

    @Min(value = 1, message = "Duration must be at least 1 month")
    private int durationMonths = 1;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1.00", message = "Price must be greater than 0")
    private BigDecimal price;

    public MembershipPlan() { }

    public MembershipPlan(String name, int durationMonths, BigDecimal price) {
        this.name = name;
        this.durationMonths = durationMonths;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
