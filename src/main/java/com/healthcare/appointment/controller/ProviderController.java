package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.ProviderRequest;
import com.healthcare.appointment.dto.ProviderResponse;
import com.healthcare.appointment.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providers;

    public ProviderController(ProviderService providers) {
        this.providers = providers;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderResponse create(@Valid @RequestBody ProviderRequest req) {
        return providers.create(req);
    }

    @PutMapping("/{id}")
    public ProviderResponse update(@PathVariable Long id,
                                   @Valid @RequestBody ProviderRequest req) {
        return providers.update(id, req);
    }

    @GetMapping("/{id}")
    public ProviderResponse get(@PathVariable Long id) {
        return providers.get(id);
    }

    @GetMapping
    public List<ProviderResponse> list(
            @RequestParam(required = false) String specialization) {
        return providers.list(specialization);
    }
}
