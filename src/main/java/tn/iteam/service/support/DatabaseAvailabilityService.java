package tn.iteam.service;

import org.springframework.stereotype.Service;
import tn.iteam.service.SourceAvailabilityService;

@Service
public class DatabaseAvailabilityService {

    private final SourceAvailabilityService availabilityService;
    private volatile boolean available = false;

    public DatabaseAvailabilityService(SourceAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    public boolean isAvailable() {
        return available;
    }

    public void markUnavailable(Throwable ex) {
        available = false;
        availabilityService.markUnavailable(
                "DATABASE",
                ex != null && ex.getMessage() != null ? ex.getMessage() : "Database unavailable"
        );
    }

    public void markAvailable() {
        available = true;
        availabilityService.markAvailable("DATABASE");
    }
}