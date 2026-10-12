package com.jobbridge.controller;

import com.jobbridge.model.Job;
import com.jobbridge.repo.JobRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/jobs")
public class AdminJobsApiController {
    private final JobRepository jobs;

    public AdminJobsApiController(JobRepository jobs) {
        this.jobs = jobs;
    }

    @GetMapping
    public List<JobsApiController.JobResponse> jobs() {
        return jobs.findAll().stream().map(AdminJobsApiController::toResponse).toList();
    }

    @PutMapping("/{id}")
    public JobsApiController.JobResponse update(
            @PathVariable Long id,
            @Valid @RequestBody JobsApiController.JobRequest request) {
        Job job = findJob(id);
        job.setTitle(request.title().trim());
        job.setCompany(request.company().trim());
        job.setLocation(request.location().trim());
        job.setJobType(request.jobType().trim());
        job.setSalary(request.salary().trim());
        job.setDescription(request.description().trim());
        return toResponse(jobs.save(job));
    }

    @PatchMapping("/{id}/publication")
    public JobsApiController.JobResponse setPublished(
            @PathVariable Long id,
            @Valid @RequestBody PublicationRequest request) {
        Job job = findJob(id);
        job.setPublished(request.published());
        return toResponse(jobs.save(job));
    }

    private Job findJob(Long id) {
        return jobs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
    }

    private static JobsApiController.JobResponse toResponse(Job job) {
        return new JobsApiController.JobResponse(job.getId(), job.getTitle(), job.getCompany(),
                job.getLocation(), job.getJobType(), job.getSalary(), job.getDescription(),
                job.getCreatedAt(), job.isPublished());
    }

    public record PublicationRequest(boolean published) {}
}
