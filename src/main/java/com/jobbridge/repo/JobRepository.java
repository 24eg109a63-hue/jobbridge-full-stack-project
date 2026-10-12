package com.jobbridge.repo;
import com.jobbridge.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface JobRepository extends JpaRepository<Job,Long> {
 @Query("select j from Job j where (j.published = true or j.published is null) and (lower(j.title) like lower(concat('%',:q,'%')) or lower(j.company) like lower(concat('%',:q,'%')) or lower(j.location) like lower(concat('%',:q,'%')))")
 List<Job> search(@Param("q") String q);
 @Query("select j from Job j where j.published = true or j.published is null")
 List<Job> findPublishedJobs();
}
