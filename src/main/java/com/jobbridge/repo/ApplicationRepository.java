package com.jobbridge.repo;
import com.jobbridge.model.JobApplication;
import com.jobbridge.model.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApplicationRepository extends JpaRepository<JobApplication,Long> {
 boolean existsByUserIdAndJobId(Long userId,Long jobId);
 List<JobApplication> findByUserOrderByAppliedAtDesc(PortalUser user);
}
