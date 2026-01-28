package com.opd.opd_token_engine.repository;

import com.opd.opd_token_engine.model.Doctor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStore {

    public static Map<String, Doctor> doctors = new ConcurrentHashMap<>();
}
