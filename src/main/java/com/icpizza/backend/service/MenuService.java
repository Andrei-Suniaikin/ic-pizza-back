package com.icpizza.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.UpdateAvailabilityRequest;
import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.ExtraIngr;
import com.icpizza.backend.entity.JahezMenu;
import com.icpizza.backend.entity.MenuItem;
import com.icpizza.backend.jahez.api.JahezApi;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.mapper.MenuMapper;
import com.icpizza.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuItemRepository menuRepository;
    private final ExtraIngrRepository ingrRepository;
    private final AtomicLong versionGen = new AtomicLong(0);
    private final MenuMapper menuMapper;
    private final JahezMenuRepository jahezMenuRepository;
    private final JahezApi jahezApi;
    private final ObjectMapper objectMapper;
    private final BranchAvailabilityRepository branchAvailabilityRepository;
    private final BranchRepository branchRepository;

    @Cacheable(cacheNames = "menu", key = "#branchId")
    public MenuSnapshot getMenu(UUID branchId) {
        return buildSnapshot(branchId);
    }

    @CachePut(cacheNames = "menu", key = "#branchId")
    public MenuSnapshot reloadMenu(UUID branchId) {
        return buildSnapshot(branchId);
    }

    private MenuSnapshot buildSnapshot(UUID branchId) {
        List<MenuItem> itemDTOs = branchAvailabilityRepository.findAllByBranchIdByOrderByIdAsc(branchId).stream()
                .map(menuItem -> MenuItem.builder()
                        .id(menuItem.getItem().getId())
                        .category(menuItem.getItem().getCategory())
                        .name(menuItem.getItem().getName())
                        .size(menuItem.getItem().getSize())
                        .price(menuItem.getItem().getPrice())
                        .photo(menuItem.getItem().getPhoto())
                        .description(menuItem.getItem().getDescription())
                        .available(menuItem.getIsAvailable())
                        .isBestSeller(menuItem.getItem().isBestSeller())
                        .externalId(menuItem.getItem().getExternalId())
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

//    public Optional<JahezDTOs.DataForJahezOrder> getItemDataForJahezOrderByExternalId(String externalId) {
//        var snap = getMenu();
//        String key = externalId == null ? null : externalId.trim();
//        return snap.itemByExt(key).map(i ->
//                new JahezDTOs.DataForJahezOrder(i.getName(), i.getCategory(), i.getSize()));
//    }
//
//    public Optional<String> getExtraNameByExternalId(String externalId) {
//        var snap = getMenu();
//        String key = externalId == null ? null : externalId.trim();
//        return snap.extraByExt(key).map(ExtraIngr::getName);
//    }
//
//    private static String nvl(String s) { return s == null ? "" : s; }

    @Transactional
    public Integer updateAvailability(UpdateAvailabilityRequest request) {
        if (request == null || request.changes()==null || request.changes().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

        int result = 0;

        for(UpdateAvailabilityRequest.Change change: request.changes()){
            switch (change.type()){
                case "group" -> {
                    result+= branchAvailabilityRepository.updateAvailableByNameIgnoreCase(change.name().toLowerCase(),
                                                                        change.enabled(), request.branchId());
                }
                case "dough" -> {
                    if(change.name().equalsIgnoreCase("Brick Dough")){
                        result+=branchAvailabilityRepository.updateAvailableByCategoryIgnoreCase("Brick Pizzas", change.enabled(), request.branchId());
                        break;
                    }
                    result += branchAvailabilityRepository.updateAvailableBySize(change.name(), change.enabled(), request.branchId());
                }
                default -> throw new IllegalArgumentException("Unknown change type: " + change.type());
            }
        }

        MenuSnapshot menu = reloadMenu(request.branchId());
        pushToJahez(menu, request.branchId());
        return  result;
    }

    public HashMap<String, Object> getBaseAppInfo(UUID branchId) {
        return menuMapper.toBaseInfoSnakeCase(getMenu(branchId));
    }

    public void pushToJahez(MenuSnapshot menu, UUID branchId) {
        try {
            Set<String> disabledExtIds = menu.getItems().stream()
                    .filter(item -> !item.isAvailable())
                    .map(MenuItem::getExternalId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<Branch> excludeBranches = branchRepository.findAllExcludeBranches(branchId);

            Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));

            String suffix = "-" + branch.getExternalId().toUpperCase();

            List<JahezMenu> baseJsons = jahezMenuRepository.findAll();

            ArrayNode productsArray = objectMapper.createArrayNode();

            for (JahezMenu jahezMenu : baseJsons) {
                JsonNode root = objectMapper.readTree(jahezMenu.getJson());
                JsonNode patched = patchJson(root, disabledExtIds, menu, excludeBranches, branch, suffix);
                log.info("[JAHEZ MENU] successfully updated products "+patched+"");
                ArrayNode products = (ArrayNode) patched.get("products");
                for (JsonNode p : products) {
                    productsArray.add(p);
                }
            }

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("products", productsArray);

            jahezApi.pushMenuToJahez(wrapper);

            log.info("Successfully pushed {} products to Jahez", productsArray.size());
        } catch (Exception e) {
            log.error("Failed to push menu to Jahez", e);
        }
    }


    public JsonNode patchJson(JsonNode root, Set<String> disabledExtIds, MenuSnapshot menu,  List<Branch> otherBranches, Branch branch, String suffix) {
        ObjectNode patched = root.deepCopy();

        if(patched.has("products")){
            ArrayNode products = (ArrayNode) patched.get("products");

            for(JsonNode product: products){
                ObjectNode prodNode = (ObjectNode) product;
                String productId = prodNode.get("product_id").asText();

                ArrayNode excludeArray = objectMapper.createArrayNode();

                otherBranches.forEach(b -> excludeArray.add(b.getExternalId()));

                if (disabledExtIds.contains(productId)) {
                    excludeArray.add(branch.getExternalId());
                }

                prodNode.set("exclude_branches", excludeArray);

                prodNode.remove("product_id");
                prodNode.put("product_id", productId + suffix);

                if(prodNode.has("modifiers")){
                    ArrayNode modifiers = (ArrayNode) prodNode.get("modifiers");

                    for(JsonNode mod: modifiers){
                        ObjectNode modNode = (ObjectNode) mod;
                        if(modNode.has("options")){
                            ArrayNode options = (ArrayNode) modNode.get("options");
                            ArrayNode filtered = objectMapper.createArrayNode();
                            for (JsonNode option : options) {
                                if (!disabledExtIds.contains(option.get("id").asText())) {
                                    filtered.add(option);
                                }
                            }
                            modNode.set("options", filtered);
                        }
                    }
                }
            }
        }

        if (allDisabled(menu, "Pizzas", "S")) {
            disableDoughAndCombo(patched, "S", branch.getExternalId());
        }
        if (allDisabled(menu, "Pizzas", "M")) {
            disableDoughAndCombo(patched, "M", branch.getExternalId());
        }
        if (allDisabled(menu, "Pizzas", "L")) {
            disableDoughAndCombo(patched, "L", branch.getExternalId());
        }

        if (allDisabled(menu, "Brick Pizzas", null)) {
            disableDetroitCombo(patched, branch.getExternalId());
        }

        return patched;
    }

    private boolean allDisabled(MenuSnapshot menu, String category, String size) {
        return menu.getItems().stream()
                .filter(it -> it.getCategory().equalsIgnoreCase(category))
                .filter(it -> size == null || size.equalsIgnoreCase(it.getSize()))
                .allMatch(it -> !it.isAvailable());
    }

    private void disableDoughAndCombo(ObjectNode root, String size, String currentBranchExtId) {
        ArrayNode products = (ArrayNode) root.get("products");
        for (JsonNode product : products) {
            ObjectNode prodNode = (ObjectNode) product;
            String pid = prodNode.get("product_id").asText();

            if (pid.startsWith("COMBO-PIZZA-" + size)) {
                if (!prodNode.has("exclude_branches")) {
                    prodNode.set("exclude_branches", objectMapper.createArrayNode());
                }

                ArrayNode excludeArray = (ArrayNode) prodNode.get("exclude_branches");

                addUnique(excludeArray, currentBranchExtId);
            }

            if (prodNode.has("modifiers")) {
                ArrayNode modifiers = (ArrayNode) prodNode.get("modifiers");
                ArrayNode filtered = objectMapper.createArrayNode();
                for (JsonNode mod : modifiers) {
                    if (!mod.get("id").asText().equalsIgnoreCase("DOUGH-MOD")) {
                        filtered.add(mod);
                    }
                }
                prodNode.set("modifiers", filtered);
            }
        }
    }

    private void disableDetroitCombo(ObjectNode root, String currentBranchExtId) {
        ArrayNode products = (ArrayNode) root.get("products");
        for (JsonNode product : products) {
            ObjectNode prodNode = (ObjectNode) product;
            if (prodNode.get("product_id").asText().startsWith("COMBO-DETROIT")) {
                if (!prodNode.has("exclude_branches")) {
                    prodNode.set("exclude_branches", objectMapper.createArrayNode());
                }
                ArrayNode excludeArray = (ArrayNode) prodNode.get("exclude_branches");
                addUnique(excludeArray, currentBranchExtId);
            }
        }
    }

    private void addUnique(ArrayNode array, String value) {
        for (JsonNode node : array) {
            if (node.asText().equals(value)) {
                return;
            }
        }
        array.add(value);
    }
}
