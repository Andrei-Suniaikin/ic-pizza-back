package com.icpizza.backend.management.service;

import com.icpizza.backend.management.dto.VendorTO;
import com.icpizza.backend.management.entity.Vendor;
import com.icpizza.backend.management.mapper.VendorMapper;
import com.icpizza.backend.management.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {
    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    public List<VendorTO> getAllVendors(){
        List<Vendor> vendors = vendorRepository.findAll();
        return vendorMapper.toVendorTOS(vendors);
    }
}
