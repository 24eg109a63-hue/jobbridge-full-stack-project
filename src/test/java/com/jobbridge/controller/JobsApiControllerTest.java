package com.jobbridge.controller;

import com.jobbridge.model.Job;
import com.jobbridge.model.JobApplication;
import com.jobbridge.model.PortalUser;
import com.jobbridge.repo.ApplicationRepository;
import com.jobbridge.repo.JobRepository;
import com.jobbridge.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class JobsApiControllerTest {
    private final JobRepository jobs = mock(JobRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final UserRepository users = mock(UserRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final ApplicationRepository applications = mock(
            ApplicationRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final JobsApiController controller = new JobsApiController(jobs, users, applications);

    @Test
    void unpublishedJobIsHiddenAndCannotReceiveApplications() {
        Job job = new Job();
        job.setPublished(false);
        when(jobs.findById(17L)).thenReturn(Optional.of(job));

        ResponseStatusException detailError = assertThrows(
                ResponseStatusException.class, () -> controller.job(17L));
        assertEquals(HttpStatus.NOT_FOUND.value(), detailError.getStatusCode().value());

        PortalUser user = new PortalUser();
        when(users.findByEmailIgnoreCase("qa@example.com")).thenReturn(Optional.of(user));
        Authentication authentication = mock(Authentication.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(authentication.getName()).thenReturn("qa@example.com");

        ResponseStatusException applicationError = assertThrows(
                ResponseStatusException.class, () -> controller.apply(17L, authentication));
        assertEquals(HttpStatus.NOT_FOUND.value(), applicationError.getStatusCode().value());
        verify(applications, never()).save(any(JobApplication.class));
    }

    @Test
    void publishedJobRemainsAvailableById() {
        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setCompany("Example Co");
        job.setLocation("Remote");
        job.setJobType("Full-time");
        job.setSalary("₹5–8 LPA");
        job.setDescription("Build APIs.");
        when(jobs.findById(23L)).thenReturn(Optional.of(job));

        JobsApiController.JobResponse response = controller.job(23L);

        assertEquals("Backend Engineer", response.title());
        assertEquals("Example Co", response.company());
        assertEquals(true, response.published());
    }
}
