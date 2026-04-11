package com.woobeee.artmarketplace.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class Address {


    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String recipientName;   // 수령인
    private String phoneNumber;

    private String zipcode;
    private String address1;
    private String address2;

    private boolean isDefault;

    private LocalDateTime createdAt;
    private UUID memberId;

}
