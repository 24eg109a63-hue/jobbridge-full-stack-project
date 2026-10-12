package com.jobbridge.controller;

import com.jobbridge.model.Job;
import com.jobbridge.model.JobApplication;
import com.jobbridge.model.PortalUser;
import com.jobbridge.repo.ApplicationRepository;
import com.jobbridge.repo.JobRepository;
import com.jobbridge.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class JobsApiController {
    private final JobRepository jobs;
    private final UserRepository users;
    private final ApplicationRepository applications;

    public JobsApiController(JobRepository jobs, UserRepository users, ApplicationRepository applications) {
        this.jobs = jobs;
        this.users = users;
        this.applications = applications;
    }

    @GetMapping("/jobs")
    public List<JobResponse> jobs(@RequestParam(defaultValue = "") String q) {
        List<Job> results = q.isBlank() ? jobs.findPublishedJobs() : jobs.search(q.trim());
        return results.stream().map(JobResponse::from).toList();
    }

    @GetMapping("/jobs/{id}")
    public JobResponse job(@PathVariable Long id) {
        return JobResponse.from(findPublishedJob(id));
    }

    @PostMapping("/jobs")
    public JobResponse createJob(@Valid @RequestBody JobRequest request) {
        Job job = new Job();
        job.setTitle(request.title().trim());
        job.setCompany(request.company().trim());
        job.setLocation(request.location().trim());
        job.setJobType(request.jobType().trim());
        job.setSalary(request.salary().trim());
        job.setDescription(request.description().trim());
        return JobResponse.from(jobs.save(job));
    }

    @PostMapping("/jobs/{id}/applications")
    public ApplicationResponse apply(@PathVariable Long id, Authentication authentication) {
        PortalUser user = findUser(authentication);
        Job job = findPublishedJob(id);
        if (!applications.existsByUserIdAndJobId(user.getId(), job.getId())) {
            applications.save(new JobApplication(user, job));
        }
        return new ApplicationResponse(job.getId(), job.getTitle(), job.getCompany(), job.getLocation(), LocalDateTime.now());
    }

    @GetMapping("/applications")
    public List<ApplicationResponse> applications(Authentication authentication) {
        PortalUser user = findUser(authentication);
        return applications.findByUserOrderByAppliedAtDesc(user).stream()
                .map(application -> new ApplicationResponse(
                        application.getJob().getId(),
                        application.getJob().getTitle(),
                        application.getJob().getCompany(),
                        application.getJob().getLocation(),
                        application.getAppliedAt()))
                .toList();
    }

    private Job findJob(Long id) {
        return jobs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
    }

    private Job findPublishedJob(Long id) {
        Job job = findJob(id);
        if (!job.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found.");
        }
        return job;
    }

    private PortalUser findUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    public record JobRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String title,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String company,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String location,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 40) String jobType,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 30) String salary,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 3000) String description) {}

    public record JobResponse(
            Long id, String title, String company, String location, String jobType,
            String salary, String description, LocalDateTime createdAt, boolean published) {
        static JobResponse from(Job job) {
            return new JobResponse(job.getId(), job.getTitle(), job.getCompany(), job.getLocation(),
                    job.getJobType(), job.getSalary(), job.getDescription(), job.getCreatedAt(), job.isPublished());
        }
    }

    public record ApplicationResponse(
            Long jobId, String title, String company, String location, LocalDateTime appliedAt) {}
}
