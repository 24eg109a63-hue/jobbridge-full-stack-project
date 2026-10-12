package com.jobbridge.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
@Entity
public class Job {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank @Size(max=100) private String title;
 @NotBlank @Size(max=100) private String company;
 @NotBlank @Size(max=100) private String location;
 @NotBlank @Size(max=40) private String jobType;
 @NotBlank @Size(max=30) private String salary;
 @NotBlank @Size(max=3000) @Column(length=3000) private String description;
 private LocalDateTime createdAt=LocalDateTime.now();
 @Column(nullable=false, columnDefinition="boolean default true")
 private Boolean published=true;
 public Long getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getCompany(){return company;} public void setCompany(String v){company=v;}
 public String getLocation(){return location;} public void setLocation(String v){location=v;}
 public String getJobType(){return jobType;} public void setJobType(String v){jobType=v;}
 public String getSalary(){return salary;} public void setSalary(String v){salary=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public LocalDateTime getCreatedAt(){return createdAt;}
 public boolean isPublished(){return !Boolean.FALSE.equals(published);}
 public void setPublished(boolean v){published=v;}
}
