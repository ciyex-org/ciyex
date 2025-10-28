package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Communication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunicationRepository extends JpaRepository<Communication, Long> {
}