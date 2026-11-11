package com.shoplab.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * A REPOSITORY: Spring Data writes the implementation for us at startup.
 * JpaRepository gives findById, findAll, save, delete ...;
 * JpaSpecificationExecutor allows dynamic filters (see ProductSpecifications).
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("select distinct p.category from Product p order by p.category")
    List<String> findAllCategories();
}
