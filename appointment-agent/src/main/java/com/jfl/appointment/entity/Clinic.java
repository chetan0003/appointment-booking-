package com.jfl.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "clinic")
@Getter
@Setter
@NoArgsConstructor
public class Clinic extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "state")
    private String state;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    // Optional geo location
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

//    @Column(name = "created_at", updatable = false, insertable = false)
//    private LocalDateTime createdAt;
}
