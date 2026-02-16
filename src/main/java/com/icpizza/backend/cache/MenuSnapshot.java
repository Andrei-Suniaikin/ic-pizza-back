package com.icpizza.backend.cache;

import com.icpizza.backend.entity.BranchAvailability;
import com.icpizza.backend.entity.ExtraIngr;
import com.icpizza.backend.entity.MenuItem;
import com.icpizza.backend.entity.Topping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuSnapshot {
    private List<MenuItem> items;
    private List<ExtraIngr> extras;
    private List<Topping> toppings;
    private Instant generatedAt;
    private long version;

    @Builder.Default
    private final Map<String, MenuItem> itemsByExternalId = Map.of();

    @Builder.Default
    private final Map<String, ExtraIngr> extrasByExternalId = Map.of();


    public Optional<MenuItem> itemByExt(String externalId) {
        return Optional.ofNullable(itemsByExternalId.get(externalId));
    }

    public Optional<ExtraIngr> extraByExt(String externalId) {
        return Optional.ofNullable(extrasByExternalId.get(externalId));
    }

    private static String norm(String id) {
        return id == null ? null : id.trim();
    }
}
