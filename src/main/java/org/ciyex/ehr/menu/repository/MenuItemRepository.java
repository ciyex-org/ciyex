package org.ciyex.ehr.menu.repository;

import org.ciyex.ehr.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByMenuIdOrderByPosition(UUID menuId);

    List<MenuItem> findByMenuIdAndParentIdOrderByPosition(UUID menuId, UUID parentId);

    List<MenuItem> findByMenuIdAndParentIdIsNullOrderByPosition(UUID menuId);

    void deleteByMenuId(UUID menuId);
}
