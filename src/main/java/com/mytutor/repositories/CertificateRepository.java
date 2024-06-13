/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mytutor.repositories;

import com.mytutor.entities.Certificate;
import java.util.List;

import com.mytutor.entities.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author vothimaihoa
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    List<Certificate> findByAccountId(Integer tutorId);

    @Query("SELECT c FROM Certificate c WHERE c.account.id = :accountId AND c.isVerified = :isVerified")
    List<Certificate> findByAccountId(@Param("accountId") Integer tutorId, @Param("isVerified") boolean isVerified);
}
