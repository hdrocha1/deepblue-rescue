package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "treatments")
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TreatmentType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    protected Treatment() {}

    public Treatment(LocalDateTime performedAt, TreatmentType type, String description) {
        this.performedAt = performedAt;
        this.type = type;
        this.description = description;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
        if (animal != null && !animal.getTreatments().contains(this)) {
            animal.getTreatments().add(this);
        }
    }

    public void setSpecialist(Specialist specialist) {
        this.specialist = specialist;
        if (specialist != null && !specialist.getTreatments().contains(this)) {
            specialist.getTreatments().add(this);
        }
    }

    public Long getId() { return id; }
    public Animal getAnimal() { return animal; }
    public Specialist getSpecialist() { return specialist; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public TreatmentType getType() { return type; }
    public String getDescription() { return description; }

    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
    public void setType(TreatmentType type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
}
