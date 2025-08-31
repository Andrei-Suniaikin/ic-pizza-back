package com.icpizza.backend.service;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.UpdateAvailabilityRequest;
import com.icpizza.backend.entity.ExtraIngr;
import com.icpizza.backend.entity.MenuItem;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.mapper.MenuMapper;
import com.icpizza.backend.repository.ExtraIngrRepository;
import com.icpizza.backend.repository.MenuItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuItemRepository menuRepository;
    private final ExtraIngrRepository ingrRepository;
    private final AtomicLong versionGen = new AtomicLong(0);
    private final MenuMapper menuMapper;

    @Cacheable(cacheNames = "menu", key = "'all'")
    public MenuSnapshot getMenu() {
        return buildSnapshot();
    }

    @CachePut(cacheNames = "menu", key = "'all'")
    public MenuSnapshot reloadMenu() {
        return buildSnapshot();
    }

    private MenuSnapshot buildSnapshot() {
        List<MenuItem> itemDTOs = menuRepository.findAll().stream()
                .map(menuItem -> MenuItem.builder()
                        .id(menuItem.getId())
                        .category(menuItem.getCategory())
                        .name(menuItem.getName())
                        .size(menuItem.getSize())
                        .price(menuItem.getPrice())
                        .photo(menuItem.getPhoto())
                        .description(menuItem.getDescription())
                        .available(menuItem.isAvailable())
                        .isBestSeller(menuItem.isBestSeller())
                        .externalId(menuItem.getExternalId())
                        .build())
                .toList();

        List<ExtraIngr> extraDTOs = ingrRepository.findAll().stream()
                .map(extraIngr -> ExtraIngr.builder()
                        .id(extraIngr.getId())
                        .name(extraIngr.getName())
                        .photo(extraIngr.getPhoto())
                        .price(extraIngr.getPrice())
                        .size(extraIngr.getSize())
                        .available(extraIngr.isAvailable())
                        .externalId(extraIngr.getExternalId())
                        .build())
                .toList();

        Map<String, MenuItem> itemsByExt = itemDTOs.stream()
                .filter(i -> i.getExternalId() != null && !i.getExternalId().isBlank())
                .collect(Collectors.toUnmodifiableMap(
                        i -> i.getExternalId().trim(),
                        Function.identity(), (a,b) -> a));

        Map<String, ExtraIngr> extrasByExt = extraDTOs.stream()
                .filter(e -> e.getExternalId() != null && !e.getExternalId().isBlank())
                .collect(Collectors.toUnmodifiableMap(
                        i -> i.getExternalId().trim(),
                        Function.identity(), (a, b) -> a));

        return MenuSnapshot.builder()
                .items(itemDTOs)
                .extras(extraDTOs)
                .generatedAt(Instant.now())
                .itemsByExternalId(itemsByExt)
                .extrasByExternalId(extrasByExt)
                .version(versionGen.incrementAndGet())
                .build();
    }

    public Optional<JahezDTOs.DataForJahezOrder> getItemDataForJahezOrderByExternalId(String externalId) {
        var snap = getMenu();
        String key = externalId == null ? null : externalId.trim();
        return snap.itemByExt(key).map(i ->
                new JahezDTOs.DataForJahezOrder(i.getName(), i.getCategory(), i.getSize()));
    }

    /** По option.id из Jahez → имя доп. ингредиента. */
    public Optional<String> getExtraNameByExternalId(String externalId) {
        var snap = getMenu();
        String key = externalId == null ? null : externalId.trim();
        return snap.extraByExt(key).map(ExtraIngr::getName);
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    @Transactional
    public Integer updateAvailability(UpdateAvailabilityRequest request) {
        if (request == null || request.changes()==null || request.changes().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

        int result = 0;

        for(UpdateAvailabilityRequest.Change change: request.changes()){
            switch (change.type()){
                case "group" -> {
                    result+= menuRepository.updateAvailableByNameIgnoreCase(change.name().toLowerCase(),
                                                                        change.enabled());
                }
                case "dough" -> {
                    if(change.name().equalsIgnoreCase("Brick Dough")){
                        result+=menuRepository.updateAvailableByCategoryIgnoreCase("Brick Pizzas", change.enabled());
                        break;
                    }
                    result += menuRepository.updateAvailableBySize(change.name(), change.enabled());
                }
                default -> throw new IllegalArgumentException("Unknown change type: " + change.type());
            }
        }

        reloadMenu();
        return  result;
    }

    public HashMap<String, Object> getBaseAppInfo() {
        return menuMapper.toBaseInfoSnakeCase(getMenu());
    }
}
