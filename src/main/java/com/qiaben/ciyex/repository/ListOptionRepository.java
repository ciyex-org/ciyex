package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ListOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListOptionRepository extends JpaRepository<ListOption, Long> {

    // Custom queries can be added if needed, for example:
    ListOption findByOrgIdAndListIdAndOptionId(String orgId, String listId, String optionId);


    // Custom query to fetch list options by list_id
    List<ListOption> findByListId(String listId);
    void deleteByListId(String listId);
}
