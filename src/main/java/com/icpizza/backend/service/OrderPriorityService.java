package com.icpizza.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPriorityService {
    private static final String KEY = "priority";
    private final com.github.benmanes.caffeine.cache.Cache<String, List<Long>> cache;
    private final java.util.concurrent.locks.ReadWriteLock rw = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public List<Long> replaceAll(java.util.List<Long> idsInOrder) {
        rw.writeLock().lock();
        try {
            var src = (idsInOrder == null) ? java.util.List.<Long>of() : idsInOrder;
            var clean = src.stream().filter(java.util.Objects::nonNull).distinct().toList();
            cache.put(KEY, new java.util.ArrayList<>(clean));
            return List.copyOf(clean);
        } finally { rw.writeLock().unlock(); }
    }

    public void appendIfAbsent(Long id) {
        if (id == null) return;
        rw.writeLock().lock();
        try {
            var cur = cache.getIfPresent(KEY);
            if (!cur.contains(id)) cur.add(id);
        } finally { rw.writeLock().unlock(); }
    }

    public void remove(Long id) {
        if (id == null) return;
        rw.writeLock().lock();
        try {
            var cur = cache.getIfPresent(KEY);
            if (cur != null) cur.remove(id);
        } finally { rw.writeLock().unlock(); }
    }

    public java.util.List<Long> currentOrder() {
        rw.readLock().lock();
        try {
            var cur = cache.getIfPresent(KEY);
            return (cur == null) ? java.util.List.of() : java.util.List.copyOf(cur);
        } finally { rw.readLock().unlock(); }
    }
}
