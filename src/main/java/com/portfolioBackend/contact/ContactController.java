package com.portfolioBackend.contact;

import com.portfolioBackend.notifications.OwnerAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contact")
public class ContactController {

    private final OwnerAlertService ownerAlertService;

    public ContactController(OwnerAlertService ownerAlertService) {
        this.ownerAlertService = ownerAlertService;
    }

    @PostMapping
    public ResponseEntity<Void> contact(@RequestBody ContactRequest request) {
        String name    = request.name()    == null ? "" : request.name().trim();
        String email   = request.email()   == null ? "" : request.email().trim();
        String message = request.message() == null ? "" : request.message().trim();

        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ownerAlertService.notifyContactForm(name, email, message);
        return ResponseEntity.noContent().build();
    }
}
