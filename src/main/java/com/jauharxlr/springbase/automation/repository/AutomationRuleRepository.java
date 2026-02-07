package com.jauharxlr.springbase.automation.repository;

import com.jauharxlr.springbase.automation.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {
    List<AutomationRule> findByProjectRef(String projectRef);
    List<AutomationRule> findByProjectRefAndTableNameAndEvent(String projectRef, String tableName, String event);
}
