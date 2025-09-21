package com.ebingo.backend.game.ws;

import java.util.HashSet;
import java.util.Set;

public class IdCollisionTest {

    // Example alphabet: you can replace this with your own
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        int numIds = 1_000;  // Number of IDs to generate
        int idLength = 10;         // Length of each ID
        Set<String> ids = new HashSet<>();
        int collisions = 0;

        for (int i = 0; i < numIds; i++) {
            String id = generateId(idLength);
            if (!ids.add(id)) {
                collisions++;
            }
        }

        System.out.println("Generated " + numIds + " IDs");
        System.out.println("Collisions found: " + collisions);
        System.out.println("Collision rate: " + ((double) collisions / numIds * 100) + "%");
    }
}
