package com.icpizza.backend.management.mapper;

import com.icpizza.backend.management.dto.VendorTO;
import com.icpizza.backend.management.entity.Vendor;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class VendorMapper {

    public List<VendorTO> toVendorTOS(List<Vendor> vendors){
        return vendors.stream().map(vendor ->
                {return new VendorTO(
                 vendor.getId(),
                 vendor.getVendorName()
                );
                }).toList();
    }
}
