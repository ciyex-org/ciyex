package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ListOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListOptionRepository extends JpaRepository<ListOption, Long> {

    // Single tenant - no orgId needed
    ListOption findByListIdAndOptionId(String listId, String optionId);

    List<ListOption> findByListId(String listId);
    
    void deleteByListId(String listId);
}
