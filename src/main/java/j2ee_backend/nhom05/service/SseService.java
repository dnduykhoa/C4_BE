package j2ee_backend.nhom05.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    private final List<SseEmitter> broadcastEmitters = new CopyOnWriteArrayList<>();
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        if (userId == null) {
            broadcastEmitters.add(emitter);
        } else {
            userEmitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        }

        emitter.onCompletion(() -> removeEmitter(emitter, userId));
        emitter.onTimeout(() -> removeEmitter(emitter, userId));
        emitter.onError(e -> removeEmitter(emitter, userId));
        return emitter;
    }

    public void broadcastProductUpdate(Long productId, String status, Integer stockQuantity) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("status", status);
        data.put("stockQuantity", stockQuantity);

        sendToAll("product-update", data);
    }

    public void broadcastVariantUpdate(Long productId, Long variantId, Boolean isActive, Integer stockQuantity) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("variantId", variantId);
        data.put("isActive", isActive);
        data.put("stockQuantity", stockQuantity);

        sendToAll("variant-update", data);
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        if (userId == null) {
            return;
        }

        List<SseEmitter> emitters = userEmitters.getOrDefault(userId, List.of());
        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }

        emitters.removeAll(dead);
        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
    }

    private void sendToAll(String eventName, Object data) {
        List<SseEmitter> dead = new ArrayList<>();

        List<SseEmitter> allEmitters = new ArrayList<>(broadcastEmitters);
        for (List<SseEmitter> userList : userEmitters.values()) {
            allEmitters.addAll(userList);
        }

        for (SseEmitter emitter : allEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }

        for (SseEmitter emitter : dead) {
            removeEmitterFromAll(emitter);
        }
    }

    private void removeEmitter(SseEmitter emitter, Long userId) {
        if (userId == null) {
            broadcastEmitters.remove(emitter);
            return;
        }

        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    private void removeEmitterFromAll(SseEmitter emitter) {
        broadcastEmitters.remove(emitter);
        for (Map.Entry<Long, List<SseEmitter>> entry : userEmitters.entrySet()) {
            List<SseEmitter> emitters = entry.getValue();
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(entry.getKey());
            }
        }
    }
}
