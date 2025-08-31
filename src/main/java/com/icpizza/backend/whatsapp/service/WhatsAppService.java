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
import java.util.stream.Collectors;

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



    public String buildOrderMessage(Long orderId, List<OrderItem> sortedItems, BigDecimal totalAmount) {
        List<String> lines = new ArrayList<>();

        for (OrderItem it : sortedItems) {
            int qty = it.getQuantity() == null ? 1 : it.getQuantity();
            String name = safe(it.getName());
            String size = safe(it.getSize());
            String category = safe(it.getCategory());
            String desc = safe(it.getDescription());

            List<String> detailsBlock = new ArrayList<>();

            if ("Combo Deals".equals(category) && desc.contains(";")) {
                String[] parts = desc.split(";");
                for (String part : parts) {
                    String[] plus = part.trim().split("\\+");
                    String main = plus[0].trim();
                    List<String> extras = new ArrayList<>();
                    for (int i = 1; i < plus.length; i++) {
                        String p = plus[i].trim();
                        if (!p.isEmpty()) extras.add("+" + p);
                    }
                    String formatted = "    *" + main + "*\n" +
                            extras.stream().map(e -> "      " + e).collect(Collectors.joining("\n"));
                    detailsBlock.add(formatted);
                }
            } else {
                if (!desc.isEmpty()) {
                    String descClean = desc.replace(";", "");
                    for (String d : descClean.split("\\+")) {
                        String x = d.trim();
                        if (!x.isEmpty() && !"'".equals(x)) detailsBlock.add("    +" + x);
                    }
                }
            }

            String title = qty + "x *" + name + "*";
            if (!size.isEmpty()) title += " (" + size + ")";

            String full = detailsBlock.isEmpty()
                    ? title
                    : title + "\n" + String.join("\n", detailsBlock);

            lines.add(full);
        }

        String body = String.join("\n", lines);
        return ("""
                ✅ *Got it! Your order %d is confirmed!*

                %s

                💰 Total: %s BHD
                Thank you! See you soon! 🍕
                """).formatted(orderId, body, fmtMoney(totalAmount)).trim();
    }

    /** Аналог send_order_confirmation */
    public void sendOrderConfirmation(String telephoneNo, String messageBody, BigDecimal totalAmount, Long orderId) {
        Map<String, Object> payload = templatePayload(
                telephoneNo,
                "order_confirm",
                List.of(
                        headerParam("order_confirm", "✅Got it! Your order " + orderId + " is confirmed!"),
                        bodyParams(
                                textParam("total_price", fmtMoney(totalAmount)),
                                textParam("orderbody", messageBody)
                        )
                )
        );
        postMessages(payload, "sendOrderConfirmation");
    }

    public String buildKitchenMessage(List<OrderItem> sortedItems) {
        List<String> parts = new ArrayList<>();
        for (OrderItem it : sortedItems) {
            int qty = it.getQuantity() == null ? 1 : it.getQuantity();
            String name = safe(it.getName());
            String size = safe(it.getSize());
            String desc = safe(it.getDescription());

            List<String> details = new ArrayList<>();
            for (String d : desc.split("\\+")) {
                String x = d.trim();
                if (!x.isEmpty() && !"'".equals(x)) details.add(x);
            }
            String descText = details.isEmpty() ? "" : " (" + String.join(" + ", details) + ")";
            parts.add(qty + "x - *" + name + "* (" + size + ")" + descText);
        }
        return String.join(" | ", parts);
    }

    /** Аналог send_order_to_kitchen_text2 */
    public void sendOrderToKitchenText2(Long orderId, String messageBody, String telephoneNo, boolean isEdit, String name) {
        String headerText = (isEdit ? "✏️ Order " + orderId + " updated!" : "✅ New order: " + orderId + "!");
        String clientInfo = telephoneNo + " (" + (StringUtils.hasText(name) ? name : "") + ")";

        for (String phone : kitchenPhones()) {
            Map<String, Object> payload = templatePayload(
                    phone,
                    "order_info2",
                    List.of(
                            headerParam("header", headerText),
                            bodyParams(
                                    textParam("client_info", clientInfo),
                                    textParam("orderbody", messageBody)
                            )
                    )
            );
            postMessages(payload, "sendOrderToKitchenText2");
        }
    }

    public void sendReadyMessage(String recipientPhone, String userName, Object userId) {
        String name = (userName == null || userName.isBlank()) ? "Habibi" : userName;

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipientPhone);
        payload.put("type", "template");
        payload.put("template", Map.of(
                "name", "last_confirm",
                "language", Map.of("code", "en"),
                "components", List.of(
                        Map.of("type", "BODY",
                                "parameters", List.of(textParam("name", name))),
                        Map.of("type", "BUTTON",
                                "sub_type", "url",
                                "index", 0,
                                "parameters", List.of(Map.of("type", "text", "text", String.valueOf(userId))))
                )
        ));
        postMessages(payload, "sendReadyMessage");
    }

    public void sendMenuUtility(String recipientPhone, Object userId) {
        Map<String, Object> payload = templatePayload(
                recipientPhone,
                "name_confirmed22",
                List.of(
                        headerParam(null, "Habibi, order online, it's quick & super easy! 🍕"),
                        bodyParams(textParam(null, "Tested on my grandma😄")),
                        Map.of("type", "BUTTON",
                                "sub_type", "url",
                                "index", 0,
                                "parameters", List.of(Map.of("type", "text", "text", String.valueOf(userId))))
                )
        );
        postMessages(payload, "sendMenuUtility");
    }

    public void askForName(String recipientPhone) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", recipientPhone,
                "type", "text",
                "text", Map.of("body",
                        "Salam Aleikum 👋!\n" +
                                "I'm Hamoody, IC Pizza Bot 🤖\n" +
                                "Send me your name so I can share the menu with you 🍕\n")
        );
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

    private static String safe(String s) { return s == null ? "" : s; }
    private static String fmtMoney(BigDecimal v) { return v == null ? "0.000" : v.setScale(3, BigDecimal.ROUND_HALF_UP).toPlainString(); }

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
