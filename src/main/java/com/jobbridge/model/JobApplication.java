package com.jobbridge.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"user_id","job_id"}))
public class JobApplication {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private PortalUser user;
 @ManyToOne(optional=false) private Job job;
 private LocalDateTime appliedAt=LocalDateTime.now();
 protected JobApplication(){}
 public JobApplication(PortalUser user,Job job){this.user=user;this.job=job;}
 public Long getId(){return id;} public PortalUser getUser(){return user;} public Job getJob(){return job;} public LocalDateTime getAppliedAt(){return appliedAt;}
}
