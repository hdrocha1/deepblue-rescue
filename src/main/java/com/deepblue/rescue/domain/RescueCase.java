package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_code", nullable = false, unique = true, length = 50)
    private String caseCode;

    @Column(name = "rescue_date", nullable = false)
    private LocalDate rescueDate;

    @Column(name = "rescue_location", nullable = false, length = 200)
    private String rescueLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RescueStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rescue_center_id", nullable = false)
    private RescueCenter rescueCenter;

    @OneToOne(mappedBy = "rescueCase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Animal animal;

    protected RescueCase() {}

    public RescueCase(String caseCode, LocalDate rescueDate, String rescueLocation, RescueStatus status) {
        this.caseCode = caseCode;
        this.rescueDate = rescueDate;
        this.rescueLocation = rescueLocation;
        this.status = status;
    }

    public void assignAnimal(Animal animal) {
        if (this.animal == animal) return;
        this.animal = animal;
        if (animal != null && animal.getRescueCase() != this) {
            animal.setRescueCase(this);
        }
    }

    public void setRescueCenter(RescueCenter rescueCenter) {
        this.rescueCenter = rescueCenter;
        if (rescueCenter != null && !rescueCenter.getCases().contains(this)) {
            rescueCenter.getCases().add(this);
        }
    }

    public Long getId() { return id; }
    public String getCaseCode() { return caseCode; }
    public LocalDate getRescueDate() { return rescueDate; }
    public String getRescueLocation() { return rescueLocation; }
    public RescueStatus getStatus() { return status; }
    public RescueCenter getRescueCenter() { return rescueCenter; }
    public Animal getAnimal() { return animal; }

    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public void setRescueDate(LocalDate rescueDate) { this.rescueDate = rescueDate; }
    public void setRescueLocation(String rescueLocation) { this.rescueLocation = rescueLocation; }
    public void setStatus(RescueStatus status) { this.status = status; }
}
