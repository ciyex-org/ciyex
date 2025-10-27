package com.qiaben.ciyex.repository;


import com.qiaben.ciyex.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {


    @Query("SELECT COUNT(s) FROM Schedule s ")
    long count();


    List<Schedule> findAll();
}