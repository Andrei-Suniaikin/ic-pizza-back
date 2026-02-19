package com.icpizza.backend.jahez.mapper;

import com.icpizza.backend.entity.*;
import com.icpizza.backend.enums.OrderStatus;
import com.icpizza.backend.jahez.dto.JahezDTOs;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JahezOrderMapper {
    private final MenuService menuService;

    private static final String GARLIC_TRUE  = "GARLIC-TRUE";
    private static final String GARLIC_FALSE = "GARLIC-FALSE";
    private static final String THIN_TRUE    = "THIN-TRUE";
    private static final String THIN_FALSE   = "THIN-FALSE";
    private static final String PIZZA_PREFIX = "PIZZA-";
    private static final Set<String> BEVERAGE_IDS = Set.of(
            "COCA-COLA-S",
            "COCA-COLA-ZERO-S",
            "7UP-S",
            "7UP-DIET-S",
            "FANTA-ORANGE-S",
            "WATER-S",
            "KINZA-COLA-S",
            "MIRINDA-CITRUS-S"
    );
    private final BranchRepository branchRepository;

    Random random = new Random();
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TOL  = new BigDecimal("0.01");
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    public Boolean isBeverageId(String id){
        return BEVERAGE_IDS.contains(id);
    }

    public MappedOrder map(JahezDTOs.JahezOrderCreatePayload in, Order order) {
        Branch branch = branchRepository.findByExternalId(in.branch_id());
        var snap        = menuService.getMenu(branch.getId());
        var itemsByExt  = snap.getItemsByExternalId();
        var extrasByExt = snap.getExtrasByExternalId();

        var resultItems = new ArrayList<OrderItem>();
        List<ComboItem> allComboItems = new ArrayList<>();
        BigDecimal total = ZERO;

        for (var p : nvl(in.products())) {
            final String pid = normalizeId(p.product_id(), branch);
            if (pid == null || pid.isBlank()) continue;

            if (pid.startsWith("COMBO-")) {
                var comboOrderItem = mapCombo(p, order, itemsByExt);
                resultItems.add(comboOrderItem);

                List<ComboItem> comboParts = buildComboParts(p, comboOrderItem, itemsByExt, extrasByExt, branch);
                allComboItems.addAll(comboParts);

                total = total.add(comboOrderItem.getAmount());
                continue;
            }

            if (pid.startsWith("MINI-")) {
                var ramadanOrderItem = mapRamadan(p, order);
                resultItems.add(ramadanOrderItem);
                total = total.add(ramadanOrderItem.getAmount());

                continue;
            }

            var mi = itemsByExt.get(pid);
            if (mi == null) {
                log.warn("[JAHEZ] unknown product_id={}, skip", pid);
                continue;
            }

            int qty = def(p.quantity(), 1);
            boolean garlic = false;
            boolean thin   = false;
            var extras = new ArrayList<ExtraWithQty>();

            for (var m : nvl(p.modifiers())) {
                for (var o : nvl(m.options())) {
                    var oid = o.id();
                    if (oid == null || oid.isBlank()) continue;

                    if (GARLIC_TRUE.equals(oid) || (m.modifier_id().equals("GARLIC-MOD") && !oid.equals(GARLIC_FALSE))) {
                        garlic = true;
                        continue;
                    }
                    if (GARLIC_FALSE.equals(oid)) {
                        garlic = false;
                        continue;
                    }
                    if (THIN_TRUE.equals(oid)) {
                        thin = true;
                        continue;
                    }
                    if (THIN_FALSE.equals(oid)) {
                        thin = false;
                        continue;
                    }

                    var ex = extrasByExt.get(oid);
                    if (ex != null) {
                        extras.add(new ExtraWithQty(ex, def(o.quantity(), 1)));
                    } else {
                        log.warn("[JAHEZ] unknown option.id={}, ignored", oid);
                    }
                }
            }

            String desc = buildDesc(extras, garlic, thin);

            BigDecimal line = mi.getPrice().multiply(BigDecimal.valueOf(qty));
            for (var e : extras) {
                line = line.add(
                        e.extra().getPrice()
                                .multiply(BigDecimal.valueOf(e.qty()))
                                .multiply(BigDecimal.valueOf(qty))
                );
            }

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setName(mi.getName());
            oi.setCategory(mi.getCategory());
            oi.setSize(mi.getSize());
            oi.setQuantity(qty);
            oi.setGarlicCrust(garlic);
            oi.setThinDough(thin);
            oi.setDescription(desc);
            oi.setAmount(line);
            oi.setDiscountAmount(ZERO);

            resultItems.add(oi);
            total = total.add(line);
        }


        return new MappedOrder(resultItems, total, allComboItems);
    }

    public Order toJahezOrderEntity(JahezDTOs.JahezOrderCreatePayload jahezOrder){
        Order order = new Order();
        Branch branch = branchRepository.findByExternalId(jahezOrder.branch_id());
        order.setStatus(OrderStatus.toLabel(OrderStatus.KITCHEN_PHASE));
        order.setOrderNo(random.nextInt(1, 999));
        order.setAddress(null);
        order.setExternalId(jahezOrder.jahez_id());
        order.setNotes(jahezOrder.notes());
        order.setType("Jahez");
        order.setCreatedAt(LocalDateTime.now(BAHRAIN));
        order.setPaymentType(JahezDTOs.JahezOrderCreatePayload.PaymentMethod.toLabel(jahezOrder.payment_method()));
        order.setBranch(branch);
        return order;
    }

    private OrderItem mapCombo(JahezDTOs.JahezOrderCreatePayload.Item p,
                               Order order,
                               Map<String, MenuItem> itemsByExt) {
        var combo = itemsByExt.get(normalizeId(p.product_id(), order.getBranch()));
        if (combo == null)
            throw new IllegalArgumentException("Unknown combo product: " + p.product_id());

        int qty = def(p.quantity(), 1);

        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setName(combo.getName());
        oi.setCategory(combo.getCategory());
        oi.setSize(combo.getSize());
        oi.setQuantity(qty);
        oi.setGarlicCrust(false);
        oi.setThinDough(false);
        oi.setAmount(combo.getPrice().multiply(BigDecimal.valueOf(qty)));
        oi.setDiscountAmount(ZERO);
        return oi;
    }

    private OrderItem mapRamadan(JahezDTOs.JahezOrderCreatePayload.Item p,
                                 Order order){
        Map<String, Integer> pizzasForDesc = new HashMap<>();
        if (p.modifiers() != null) {
            for (var modifier : p.modifiers()) {
                if (modifier.options() != null){
                    for (var option : modifier.options()) {
                        String name = switch (option.id()){
                            case "MINI-PIZZA-PEPPERONI-S" -> "Pepperoni";
                            case "MINI-PIZZA-MARGHERITA-S"-> "Margherita";
                            case "MINI-PIZZA-SMOKED-TURKEY-MUSHROOM-S"->"Smoke Turkey & Mushroom";
                            case "MINI-PIZZA-CHICKEN-S" -> "Chicken";
                            case "MINI-PIZZA-VEGETARIAN-S" -> "Vegetarian";
                            case "MINI-PIZZA-VEGGIE-MEXICAN-S" -> "Zaatar with Labneh";
                            default -> throw new IllegalStateException("Unexpected value: " + option.id());
                        };
                        int qty = option.quantity();
                        pizzasForDesc.put(name, qty);
                    }
                }
            }
        }
        String description = pizzasForDesc.entrySet().stream()
                .map(entry -> entry.getKey() + " x" + entry.getValue())
                .collect(Collectors.joining(", "));



        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setName("Mini Pizza Box");
        oi.setCategory("Ramadan");
        oi.setDescription(description + " " + p.notes());
        oi.setAmount(p.final_price());
        oi.setDiscountAmount(ZERO);
        oi.setGarlicCrust(false);
        oi.setThinDough(false);
        oi.setQuantity(p.quantity());
        return oi;
    }

    private static <T> List<T> nvl(List<T> v) { return v == null ? List.of() : v; }
    private static int def(Integer v, int d) { return v == null ? d : v; }

    private static boolean hasOptionId(JahezDTOs.JahezOrderCreatePayload.Item p, String id) {
        for (var m : nvl(p.modifiers())) {
            for (var o : nvl(m.options())) {
                if (id.equalsIgnoreCase(o.id())) return true;
            }
        }
        return false;
    }

    private List<String> collectOptionIds(JahezDTOs.JahezOrderCreatePayload.Item p,
                                                 Predicate<String> filter,
                                          Branch branch) {
        var out = new ArrayList<String>();
        for (var m : nvl(p.modifiers())) {
            for (var o : nvl(m.options())) {
                var oid = o.id();
                if (oid != null) {
                    String cleanId = normalizeId(oid, branch);
                    if (filter.test(cleanId)) {
                        out.add(oid);
                    }
                }
            }
        }
        return out;
    }

    private static String buildDesc(List<ExtraWithQty> extras, boolean garlic, boolean thin) {
        var parts = new ArrayList<String>();
        for (var e : extras) {
            var sb = new StringBuilder("+").append(e.extra().getName());
            if (e.qty() > 1) sb.append(" x").append(e.qty());
            parts.add(sb.toString());
        }
        if (garlic) parts.add("+Garlic Crust");
        if (thin)   parts.add("+Thin");
        return String.join(" ", parts);
    }

    private static String humanize(String externalPizzaId) {
        String s = externalPizzaId.replaceFirst("^PIZZA-", "")
                .replaceAll("-[SMLX]{1,2}$", "")
                .replace('-', ' ')
                .trim()
                .toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return externalPizzaId;
        return Arrays.stream(s.split(" "))
                .map(w -> w.isEmpty()? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    private List<ComboItem> buildComboParts(
            JahezDTOs.JahezOrderCreatePayload.Item comboPayload,
            OrderItem comboOrderItem,
            Map<String, MenuItem> itemsByExt,
            Map<String, ExtraIngr> extrasByExt,
            Branch branch
    ) {
        List<ComboItem> parts = new ArrayList<>();
        var pizzaIds = collectOptionIds(comboPayload, id -> id.startsWith(PIZZA_PREFIX), branch);
        for (String id : pizzaIds) {
            var mi = itemsByExt.get(normalizeId(id, branch));

            if (mi == null) {
                log.warn("[JAHEZ] unknown combo pizza id={}, skip", id);
                continue;
            }

            boolean garlic = hasOptionId(comboPayload, GARLIC_TRUE);
            boolean thin   = hasOptionId(comboPayload, THIN_TRUE);

            ComboItem part = new ComboItem();
            part.setOrderItem(comboOrderItem);
            part.setName(mi.getName());
            part.setCategory(mi.getCategory());
            part.setSize(mi.getSize());
            part.setQuantity(1); // внутри комбо обычно 1
            part.setGarlicCrust(garlic);
            part.setThinDough(thin);
            part.setDescription(null);

            parts.add(part);
        }
        List<String> brickIds = collectOptionIds(comboPayload, id -> id.endsWith("-BRICK"), branch);
        for(String id: brickIds){
            MenuItem mi = itemsByExt.get(normalizeId(id, branch));
            if(mi==null){
                log.warn("[JAHEZ] unknown brick, skipped" + id);
                continue;
            }

            ComboItem part = new ComboItem();
            part.setOrderItem(comboOrderItem);
            part.setName(mi.getName());
            part.setCategory(mi.getCategory());
            part.setQuantity(1);

            parts.add(part);
        }
        var sauceIds = collectOptionIds(comboPayload, id -> id.endsWith("-SAUCE"), branch);
        for (String id: sauceIds){
            MenuItem mi = itemsByExt.get(normalizeId(id, branch));
            if(mi==null){
                log.warn("[JAHEZ] unknown sauce, skipped" + id);
                continue;
            }

            ComboItem part = new ComboItem();
            part.setOrderItem(comboOrderItem);
            part.setName(mi.getName());
            part.setCategory(mi.getCategory());
            part.setQuantity(1);

            parts.add(part);
        }
        List<String> beverageIds = collectOptionIds(comboPayload, this::isBeverageId, branch);
        for(String id: beverageIds){
            MenuItem mi = itemsByExt.get(normalizeId(id, branch));
            if(mi==null){
                log.warn("[JAHEZ] unknown beverage, skipped" + id);
                continue;
            }

            ComboItem part = new ComboItem();
            part.setOrderItem(comboOrderItem);
            part.setName(mi.getName());
            part.setCategory(mi.getCategory());
            part.setQuantity(1);

            parts.add(part);
        }
        return parts;
    }

    private record ExtraWithQty(ExtraIngr extra, int qty) {}

    private String normalizeId(String id, Branch branch) {
        if (id == null) return null;
        String suffix = "-" + branch.getExternalId().toUpperCase();
        if (id.endsWith(suffix)) {
            return id.substring(0, id.length() - suffix.length());
        }
        return id;
    }

    public record MappedOrder(
            List<OrderItem> items,
            BigDecimal total,
            List<ComboItem> comboItems
    ) {}
}
