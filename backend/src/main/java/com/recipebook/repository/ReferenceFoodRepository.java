package com.recipebook.repository;

import com.recipebook.model.ReferenceFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReferenceFoodRepository extends JpaRepository<ReferenceFood, String> {

    interface NameView {
        String getCode();
        String getNameDe();
    }

    @Query("SELECT f.code AS code, f.nameDe AS nameDe FROM ReferenceFood f")
    List<NameView> findAllNames();
}
