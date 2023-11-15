package com.nullers.restbookstore.shop.repositories;



import com.nullers.restbookstore.shop.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {


}