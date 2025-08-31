package com.icpizza.backend.mapper;

import com.icpizza.backend.cache.MenuSnapshot;
import com.icpizza.backend.entity.MenuItem;
import com.icpizza.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MenuMapper {
    private static String nz(String s) { return s == null ? "" : s; }
    private static boolean bool(Boolean b) { return b != null && b; }

    public HashMap<String, Object> toBaseInfoSnakeCase(MenuSnapshot menu){

        List<Map<String, Object>> menuList = menu.getItems().stream()
                .map(mi -> Map.<String, Object>of(
                        "id",             mi.getId(),
                        "category",       mi.getCategory(),
                        "name",           mi.getName(),
                        "size",           nz(mi.getSize()),
                        "price",          mi.getPrice(),
                        "photo",          mi.getPhoto(),
                        "description",    nz(mi.getDescription()),
                        "available",      mi.isAvailable(),
                        "is_best_seller", mi.isBestSeller()
                ))
                .toList();

        List<Map<String, Object>> extraIngrList = menu.getExtras().stream()
                .map(ex -> Map.<String, Object>of(
                        "id",        ex.getId(),
                        "name",      ex.getName(),
                        "photo",     ex.getPhoto(),
                        "price",     ex.getPrice(),
                        "size",      nz(ex.getSize()),
                        "available", ex.isAvailable()
                ))
                .toList();

        var resp = new HashMap<String,Object>();
        resp.put("menu", menuList);
        resp.put("extraIngr", extraIngrList);

        return resp;
    }
}
