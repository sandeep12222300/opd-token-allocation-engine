package com.opd.opd_token_engine.service;

import com.opd.opd_token_engine.model.Doctor;
import com.opd.opd_token_engine.repository.InMemoryStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;



@Component
public class SimulationService implements CommandLineRunner {


    @Override
    public void run(String... args) {

        Doctor d1 = new Doctor("D1", 1.2);
        d1.addSlot("9-10", 5);
        d1.addSlot("10-11", 5);

        Doctor d2 = new Doctor("D2", 1.0);
        d2.addSlot("9-10", 4);
        d2.addSlot("10-11", 4);

        Doctor d3 = new Doctor("D3", 0.8);
        d3.addSlot("9-10", 3);
        d3.addSlot("10-11", 3);

        InMemoryStore.doctors.put("D1", d1);
        InMemoryStore.doctors.put("D2", d2);
        InMemoryStore.doctors.put("D3", d3);

        System.out.println("Simulation initialized with 3 doctors");
    }
}
