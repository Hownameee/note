package com.example.mappingdemo.repository;

import com.example.mappingdemo.entity.DemoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DemoRepository extends JpaRepository<DemoEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO demo_table (int_col, bigint_col) VALUES (:intVal, :bigintVal)", nativeQuery = true)
    void insertNative(@Param("intVal") Long intVal, @Param("bigintVal") Long bigintVal);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO demo_table (int_col, bigint_col) VALUES (1, 9223372036854775808)", nativeQuery = true)
    void insertBigintOverflowNative();
}
