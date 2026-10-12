package com.digitalbank.service;

import com.digitalbank.dto.*;
import com.digitalbank.exception.*;
import com.digitalbank.model.Customer;
import com.digitalbank.model.KycStatus;
import com.digitalbank.repository.CustomerRepository;
import com.digitalbank.util.FingerPrint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final ModelMapper mapper;

    public CustomerCreatedResponse create(@Valid CustomerRequest request) {

        String externalId = request.getExternalId();
        String emailId = request.getEmail();

        String fp = FingerPrint.getFingerPrint(request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPhone(), request.getAddress());

        Optional<Customer> byExt = repository.findByExternalId(externalId);

        if(byExt.isPresent()) {
            Customer ex = byExt.get();
            if(fp.equals(ex.getRequestFingerPrint())) {
                return mapper.map(ex, CustomerCreatedResponse.class);
            }
            throw new ConflictException("Same externalId used with different data");
        }

        Optional<Customer> byEmail = repository.findByEmail(emailId);
        if(byEmail.isPresent()) {
            Customer ex = byEmail.get();
            if(fp.equals(ex.getRequestFingerPrint())) {
                return mapper.map(ex, CustomerCreatedResponse.class);
            }
            throw new ConflictException("Same email used with different data");
        }

        Customer entity = mapper.map(request, Customer.class);
        entity.setExternalId(externalId);
        entity.setActive(false);
        entity.setKycStatus(KycStatus.PENDING);
        entity.setRequestFingerPrint(fp);

        Customer saved = repository.saveAndFlush(entity);
        return mapper.map(saved, CustomerCreatedResponse.class);
        
    }

    public CustomerResponse getByExternalId(String externalId) {
        Customer customer = repository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with externalId: " + externalId));
        ;
        return mapper.map(customer, CustomerResponse.class);
    }

    public boolean exists(String externalId) {
        return repository.findByExternalId(externalId).isPresent();

    }

    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    public Integer updateCustomer(String id, UpdateCustomerRequest request, Integer expected) {
        Customer c = repository.findByExternalId(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));

        if (expected == null)
            throw new PreconditionRequiredException("If-Match header required");
        if (!expected.equals(c.getVersion())) {
            throw new VersionMismatchException("Stale version. Current=" + c.getVersion() + ", If-Match=" + expected);
        }

        mapper.map(request, Customer.class);
        Customer saved = repository.save(c); // Hibernate increments version
        return mapper.map(saved, CustomerCreatedResponse.class).getVersion();
    }

//    public Integer updateKycStatus(String id, String kycStatus) {
//        Customer c = repository.findByExternalId(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with externalId: " + id));
//
//
//        if ("VERIFIED".equalsIgnoreCase(kycStatus)) {
//            CustomerRegistrationRequest request =
//                    new CustomerRegistrationRequest(c.getEmail(), "default-password", c.getExternalId());
//            authServiceClient.registerCustomer(request);
//            c.setKycStatus(KycStatus.VERIFIED);
//            c.setActive(true);
//            repository.save(c);
//        }
//        return c.getVersion();
//    }

    public CustomerResponse getById(Long id) {
        return repository.findById(id)
                .map(i -> mapper.map(i, CustomerResponse.class))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with externalId: " + id));	}
}
