package com.example.smartBiz.repository;

import com.example.smartBiz.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepo extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findByKey(String key);
}
