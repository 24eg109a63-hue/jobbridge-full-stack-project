package com.jobbridge.config;
import com.jobbridge.model.Job;
import com.jobbridge.repo.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
@Configuration
public class SeedData {
 @Bean CommandLineRunner seedJobs(JobRepository repo){return args -> {
  if(repo.count()==0){
   add(repo,"Frontend Developer","Northstar Labs","Hyderabad, India","Full-time","₹5–8 LPA","Build accessible, responsive interfaces with a collaborative product team. Experience with HTML, CSS, JavaScript and React is helpful.");
   add(repo,"Java Backend Engineer","BluePeak Technologies","Hybrid · Hyderabad","Full-time","₹6–10 LPA","Develop reliable REST APIs, work with Spring Boot and relational databases, and contribute to code reviews.");
   add(repo,"Cybersecurity Analyst Intern","SecureLayer","Remote · India","Internship","₹15,000/month","Support security monitoring, vulnerability assessments and incident documentation under senior guidance.");
  }
 };}
 private void add(JobRepository r,String t,String c,String l,String type,String s,String d){Job j=new Job();j.setTitle(t);j.setCompany(c);j.setLocation(l);j.setJobType(type);j.setSalary(s);j.setDescription(d);r.save(j);}
}
