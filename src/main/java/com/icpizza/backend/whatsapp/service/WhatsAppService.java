package com.icpizza.backend.whatsapp.service;

import com.icpizza.backend.entity.OrderItem;
import com.icpizza.backend.whatsapp.config.WhatsAppApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;

@Service
public class WhatsAppService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final WhatsAppApiProperties props;
    private final RestClient client;

    public WhatsAppService(WhatsAppApiProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl("https://graph.facebook.com/" + props.version())
                .defaultHeader("Authorization", "Bearer " + props.accessToken())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }


    public String buildOrderMessage(List<OrderItem> sortedItems) {
        List<String> orderSummaryLines = new ArrayList<>();

        for (OrderItem it : sortedItems) {
            int qty = it.getQuantity() == null ? 1 : it.getQuantity();
            String name = (it.getName() == null ? "" : it.getName().trim());
            String size = (it.getSize() == null ? "" : it.getSize().trim());
            String category = (it.getCategory() == null ? "" : it.getCategory().trim());
            String desc = (it.getDescription() == null ? "" : it.getDescription().trim());

            List<String> detailsBlock = new ArrayList<>();

            if ("Combo Deals".equals(category) && desc.contains(";")) {
                String[] comboParts = desc.split(";");
                for (String part : comboParts) {
                    String[] lines = part.trim().split("\\+");
                    String main = lines[0].trim();
                    List<String> extras = new ArrayList<>();
                    for (int i = 1; i < lines.length; i++) {
                        String x = lines[i].trim();
                        if (!x.isEmpty()) extras.add("+" + x);
                    }
                    StringBuilder formatted = new StringBuilder();
                    formatted.append("    *").append(main).append("*\n");
                    for (int i = 0; i < extras.size(); i++) {
                        formatted.append("      ").append(extras.get(i));
                        if (i < extras.size() - 1) formatted.append("\n");
                    }
                    detailsBlock.add(formatted.toString());
                }
            } else {
                List<String> details = new ArrayList<>();
                if (!desc.isEmpty()) {
                    String descClean = desc.replace(";", "");
                    String[] parts = descClean.split("\\+");
                    for (String d : parts) {
                        String x = d.trim();
                        if (!x.isEmpty() && !"'".equals(x)) details.add(x);
                    }
                }
                if (!details.isEmpty()) {
                    StringBuilder block = new StringBuilder();
                    for (int i = 0; i < details.size(); i++) {
                        block.append("    +").append(details.get(i));
                        if (i < details.size() - 1) block.append("\n");
                    }
                    detailsBlock.add(block.toString());
                }
            }

            StringBuilder title = new StringBuilder();
            title.append(qty).append("x *").append(name).append("*");
            if (!size.isEmpty()) title.append(" (").append(size).append(")");

            String full = title.toString();
            if (!detailsBlock.isEmpty()) {
                full = full + "\n" + String.join("\n", detailsBlock);
            }
            orderSummaryLines.add(full);
        }

        return String.join("\n", orderSummaryLines);
    }

    public void sendOrderConfirmation(String telephoneNo, Integer orderNo, String orderItems, BigDecimal totalAmount) {
        final java.util.function.UnaryOperator<String> clean = v ->
                v == null ? "" : v.replaceAll("[\\r\\n\\t]+", " ")
                        .replaceAll(" {5,}", "    ")
                        .trim();

        String headerText = clean.apply("✅Got it! Your order " + orderNo + " is confirmed!");

        String totalParam = clean.apply(totalAmount.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString());
        String itemsInline = clean.apply(orderItems);

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", telephoneNo);
        payload.put("type", "template");

        java.util.Map<String, Object> template = new java.util.HashMap<>();
        template.put("name", "order_confirm");
        template.put("language", java.util.Map.of("code", "en"));

        java.util.List<java.util.Map<String, Object>> components = new java.util.ArrayList<>();

        components.add(java.util.Map.of(
                "type", "HEADER",
                "parameters", java.util.List.of(
                        java.util.Map.of("type", "text", "parameter_name", "order_confirm", "text", headerText)
                )
        ));

        components.add(java.util.Map.of(
                "type", "BODY",
                "parameters", java.util.List.of(
                        java.util.Map.of("type", "text", "parameter_name", "total_price", "text", totalParam),
                        java.util.Map.of("type", "text", "parameter_name", "orderbody",    "text", itemsInline)
                )
        ));

        template.put("components", components);
        payload.put("template", template);

        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            log.info("[WA client template] {}", json);
        } catch (Exception ignore) {}

        postMessages(payload, "sendOrderConfirmation");
    }

    public String buildKitchenMessage(List<OrderItem> sortedItems) {
        List<String> parts = new ArrayList<>();

        for (OrderItem it : sortedItems) {
            int qty = it.getQuantity() == null ? 1 : it.getQuantity();
            String name = it.getName() == null ? "" : it.getName();
            String size = it.getSize() == null ? "" : it.getSize();
            String desc = it.getDescription() == null ? "" : it.getDescription();

            List<String> details = new ArrayList<>();
            String[] pieces = desc.split("\\+");
            for (String p : pieces) {
                String x = p.trim();
                if (!x.isEmpty() && !"'".equals(x)) details.add(x);
            }
            String descText = details.isEmpty() ? "" : " (" + String.join(" + ", details) + ")";
            String part = qty + "x - *" + name + "* (" + size + ")" + descText;
            parts.add(part);
        }

        return String.join(" | ", parts);
    }

    public void sendOrderToKitchenText2(Integer orderNo, String messageBody, String telephoneNo, boolean isEdit, String name) {
        String headerText = (isEdit ? "✏️ Order " + orderNo + " updated!" : "✅ New order: " + orderNo + "!")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll(" {5,}", "    ")
                .trim();

        String clientInfo = (org.springframework.util.StringUtils.hasText(name) ? (telephoneNo + " (" + name + ")") : telephoneNo)
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll(" {5,}", "    ")
                .trim();

        String orderInline = (messageBody == null ? "" : messageBody)
                .replace("\t", " ")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll(" {5,}", "    ")
                .replaceAll("\\s*\\|\\s*", " | ")
                .trim();

        if (headerText.matches(".*(\\r|\\n|\\t| {5,}).*") ||
                clientInfo.matches(".*(\\r|\\n|\\t| {5,}).*") ||
                orderInline.matches(".*(\\r|\\n|\\t| {5,}).*")) {
            log.warn("[WA kitchen] illegal whitespace remains in template params: header='{}', client='{}', orderInline='{}'",
                    headerText, clientInfo, orderInline);
        }

        for (String phone : kitchenPhones()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", phone);
            payload.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", "order_info2");
            template.put("language", Map.of("code", "en"));

            List<Map<String, Object>> components = new ArrayList<>();
            components.add(Map.of(
                    "type", "HEADER",
                    "parameters", List.of(
                            Map.of("type", "text", "parameter_name", "header", "text", headerText)
                    )
            ));
            components.add(Map.of(
                    "type", "BODY",
                    "parameters", List.of(
                            Map.of("type", "text", "parameter_name", "client_info", "text", clientInfo),
                            Map.of("type", "text", "parameter_name", "orderbody", "text", orderInline)
                    )
            ));

            template.put("components", components);
            payload.put("template", template);

            try {
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
                log.info("[WA kitchen template] {}", json);
            } catch (Exception ignore) {}

            postMessages(payload, "sendOrderToKitchenText2");
        }
    }

    public void sendReadyMessage(String recipientPhone, String userName, Object userId) {
        String name = (userName == null ? "Habibi" : userName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipientPhone);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "last_confirm");
        template.put("language", Map.of("code", "en"));

        List<Map<String, Object>> components = new ArrayList<>();
        components.add(Map.of(
                "type", "BODY",
                "parameters", List.of(
                        Map.of("type", "text", "parameter_name", "name", "text", String.valueOf(name))
                )
        ));
        components.add(Map.of(
                "type", "BUTTON",
                "sub_type", "url",
                "index", 0,
                "parameters", List.of(
                        Map.of("type", "text", "text", String.valueOf(userId))
                )
        ));

        template.put("components", components);
        payload.put("template", template);

        postMessages(payload, "sendReadyMessage");
    }

    public void sendMenuUtility(String recipientPhone, Object userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipientPhone);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "name_confirmed22");
        template.put("language", Map.of("code", "en"));

        List<Map<String, Object>> components = new ArrayList<>();
        components.add(Map.of(
                "type", "HEADER",
                "parameters", List.of(
                        Map.of("type", "text", "text", "Habibi, order online, it's quick & super easy! 🍕")
                )
        ));
        components.add(Map.of(
                "type", "BODY",
                "parameters", List.of(
                        Map.of("type", "text", "text", "Tested on my grandma😄")
                )
        ));
        components.add(Map.of(
                "type", "BUTTON",
                "sub_type", "url",
                "index", 0,
                "parameters", List.of(
                        Map.of("type", "text", "text", String.valueOf(userId))
                )
        ));

        template.put("components", components);
        payload.put("template", template);

        postMessages(payload, "sendMenuUtility");
    }

    public void askForName(String recipientPhone) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipientPhone);
        payload.put("type", "text");
        payload.put("text", Map.of(
                "body",
                "Salam Aleikum 👋!\n" +
                        "I'm Hamoody, IC Pizza Bot 🤖\n" +
                        "Send me your name so I can share the menu with you 🍕\n"
        ));

        postMessages(payload, "askForName");
    }

    private void postMessages(Map<String, Object> payload, String op) {
        String path = "/" + props.phoneNumberId() + "/messages";
        try {
            String body = client.post()
                    .uri(path)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            log.info("[{}] WhatsApp OK: {}", op, body);
        } catch (Exception e) {
            log.error("[{}] WhatsApp ERROR: {}", op, e.getMessage(), e);
        }
    }


    private List<String> kitchenPhones() {
        if (!StringUtils.hasText(props.kitchenPhones())) return List.of();
        return Arrays.stream(props.kitchenPhones().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }


    private Map<String, Object> templatePayload(String to, String templateName, List<Map<String, Object>> components) {
        Map<String, Object> tpl = new HashMap<>();
        tpl.put("name", templateName);
        tpl.put("language", Map.of("code", "en"));
        tpl.put("components", components);

        Map<String, Object> p = new HashMap<>();
        p.put("messaging_product", "whatsapp");
        p.put("recipient_type", "individual");
        p.put("to", to);
        p.put("type", "template");
        p.put("template", tpl);
        return p;
    }

    private Map<String, Object> headerParam(String parameterName, String text) {
        Map<String, Object> param = new HashMap<>();
        param.put("type", "text");
        if (parameterName != null) param.put("parameter_name", parameterName);
        param.put("text", text);

        return Map.of("type", "HEADER", "parameters", List.of(param));
    }

    private Map<String, Object> bodyParams(Map<String, Object>... params) {
        return Map.of("type", "BODY", "parameters", List.of(params));
    }

    private Map<String, Object> textParam(String parameterName, String text) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "text");
        if (parameterName != null) m.put("parameter_name", parameterName);
        m.put("text", text);
        return m;
    }

}
