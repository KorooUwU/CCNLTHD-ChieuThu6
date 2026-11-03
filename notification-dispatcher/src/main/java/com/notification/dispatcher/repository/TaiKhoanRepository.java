package com.notification.dispatcher.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.notification.dispatcher.entity.TaiKhoan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, UUID> {

    Optional<TaiKhoan> findByEmail(String email);

    Optional<TaiKhoan> findByUserName(String userName);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

    /**
     * Tìm kiếm user theo userName chứa keyword (case-insensitive), tối đa 20 kết quả.
     * Không trả về email hay password — projection sang UserSummaryDTO thực hiện ở service.
     */
    @Query("SELECT u FROM TaiKhoan u WHERE LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY u.userName ASC")
    List<TaiKhoan> searchByUserNameContaining(@Param("keyword") String keyword, Pageable pageable);
}
