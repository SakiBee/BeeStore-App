package com.sakibee.repository;

import com.sakibee.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    public Cart findByProductIdAndUserId(Integer productId, Integer userId);
}
