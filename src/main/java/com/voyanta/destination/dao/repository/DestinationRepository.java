package com.voyanta.destination.dao.repository;


import com.voyanta.destination.dao.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {
    List<Destination> findByFeaturedTrue();
}