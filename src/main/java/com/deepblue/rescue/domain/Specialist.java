package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "specialists")
public class Specialist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_code", nullable = false, unique = true, length = 50)
    private String professionalCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false)
    private boolean active;

    @ManyToMany
    @JoinTable(
        name = "specialist_expertise",
        joinColumns = @JoinColumn(name = "specialist_id"),
        inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private Set<Expertise> expertiseAreas = new HashSet<>();

    @OneToMany(mappedBy = "specialist", fetch = FetchType.LAZY)
    private List<Treatment> treatments = new ArrayList<>();

    protected Specialist() {}

    public Specialist(String professionalCode, String firstName, String lastName,
                      String email, boolean active) {
        this.professionalCode = professionalCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.active = active;
    }

    public void addExpertise(Expertise expertise) {
        if (expertise == null) return;
        if (expertiseAreas.add(expertise) && !expertise.getSpecialists().contains(this)) {
            expertise.getSpecialists().add(this);
        }
    }

    public void addTreatment(Treatment treatment) {
        if (treatment == null) return;
        if (!treatments.contains(treatment)) treatments.add(treatment);
        if (treatment.getSpecialist() != this) treatment.setSpecialist(this);
    }

    public Long getId() { return id; }
    public String getProfessionalCode() { return professionalCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public Set<Expertise> getExpertiseAreas() { return expertiseAreas; }
    public List<Treatment> getTreatments() { return treatments; }

    public void setProfessionalCode(String professionalCode) { this.professionalCode = professionalCode; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setActive(boolean active) { this.active = active; }
}
