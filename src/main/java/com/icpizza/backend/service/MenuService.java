package com.icpizza.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.dto.UpdateAvailabilityRequest;
import com.icpizza.backend.entity.ExtraIngr;
import com.icpizza.backend.entity.JahezMenu;
import com.icpizza.backend.entity.MenuItem;
import com.icpizza.backend.jahez.api.JahezApi;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.mapper.MenuMapper;
import com.icpizza.backend.repository.ExtraIngrRepository;
import com.icpizza.backend.repository.JahezMenuRepository;
import com.icpizza.backend.repository.MenuItemRepository;
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

    @Cacheable(cacheNames = "menu", key = "'all'")
    public MenuSnapshot getMenu() {
        return buildSnapshot();
    }

    @CachePut(cacheNames = "menu", key = "'all'")
    public MenuSnapshot reloadMenu() {
        return buildSnapshot();
    }

    private MenuSnapshot buildSnapshot() {
        List<MenuItem> itemDTOs = menuRepository.findAllByOrderByIdAsc().stream()
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

        MenuSnapshot menu = reloadMenu();
        pushToJahez(menu);
        return  result;
    }

    public HashMap<String, Object> getBaseAppInfo() {
        return menuMapper.toBaseInfoSnakeCase(getMenu());
    }

    public void pushToJahez(MenuSnapshot menu) {
        try {
            Set<String> disabledExtIds = menu.getItems().stream()
                    .filter(item -> !item.isAvailable())
                    .map(MenuItem::getExternalId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            log.info("[STOP LIST IDs] "+disabledExtIds+"");

            List<JahezMenu> baseJsons = jahezMenuRepository.findAll();

            ArrayNode productsArray = objectMapper.createArrayNode();

            for (JahezMenu jahezMenu : baseJsons) {
                JsonNode root = objectMapper.readTree(jahezMenu.getJson());
                JsonNode patched = patchJson(root, disabledExtIds, menu);
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


    public JsonNode patchJson(JsonNode root, Set<String> disabledExtIds, MenuSnapshot menu){
        ObjectNode patched = root.deepCopy();

        if(patched.has("products")){
            ArrayNode products = (ArrayNode) patched.get("products");

            for(JsonNode product: products){
                ObjectNode prodNode = (ObjectNode) product;
                String productId = prodNode.get("product_id").asText();

                if(disabledExtIds.contains(productId)){
                    prodNode.put("is_visible", false);
                }

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
            disableDoughAndCombo(patched, "S");
        }
        if (allDisabled(menu, "Pizzas", "M")) {
            disableDoughAndCombo(patched, "M");
        }
        if (allDisabled(menu, "Pizzas", "L")) {
            disableDoughAndCombo(patched, "L");
        }

        if (allDisabled(menu, "Brick Pizzas", null)) {
            disableDetroitCombo(patched);
        }

        return patched;
    }

    private boolean allDisabled(MenuSnapshot menu, String category, String size) {
        return menu.getItems().stream()
                .filter(it -> it.getCategory().equalsIgnoreCase(category))
                .filter(it -> size == null || size.equalsIgnoreCase(it.getSize()))
                .allMatch(it -> !it.isAvailable());
    }

    private void disableDoughAndCombo(ObjectNode root, String size){
        ArrayNode products = (ArrayNode) root.get("products");
        for (JsonNode product : products) {
            ObjectNode prodNode = (ObjectNode) product;
            String pid = prodNode.get("product_id").asText();

            if (pid.equals("COMBO-PIZZA-" + size)) {
                prodNode.put("is_visible", false);
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

    private void disableDetroitCombo(ObjectNode root) {
        ArrayNode products = (ArrayNode) root.get("products");
        for (JsonNode product : products) {
            ObjectNode prodNode = (ObjectNode) product;
            if (prodNode.get("product_id").asText().startsWith("COMBO-DETROIT")) {
                prodNode.put("is_visible", false);
            }
        }
    }
}
