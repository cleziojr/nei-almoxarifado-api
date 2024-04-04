package com.ufrn.nei.almoxarifadoapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.ufrn.nei.almoxarifadoapi.entity.ItemEntity;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, Long>, PagingAndSortingRepository<ItemEntity, Long> {
    List<ItemEntity> findAllByActiveTrue();

    static Page<ItemEntity> getAllItems(Pageable pageable){
        return (Page<ItemEntity>) Pageable.unpaged();
    }

    Optional<ItemEntity> findByItemTagging(Long itemTagging);
}