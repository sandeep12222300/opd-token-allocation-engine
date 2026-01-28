package com.opd.opd_token_engine.repository;

import com.opd.opd_token_engine.model.Doctor;
import java.util.HashMap;
import java.util.Map;

public class InMemoryStore {

    public static Map<String, Doctor> doctors = new HashMap<>();
}
