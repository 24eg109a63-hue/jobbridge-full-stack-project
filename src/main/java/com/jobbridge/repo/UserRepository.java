package com.jobbridge.repo;
import com.jobbridge.model.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<PortalUser,Long> { Optional<PortalUser> findByEmailIgnoreCase(String email); }
