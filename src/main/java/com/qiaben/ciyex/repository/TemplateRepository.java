package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("SELECT t FROM Template t ")
    List<Template> findAll();
}
