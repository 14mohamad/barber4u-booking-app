package com.example.barber4u.data.repositories;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;

public class ParseRoleTest {

    private UserRepository repo;

    @Before
    public void setUp() {
        // 1) Setup conditions
        repo = new UserRepository();
    }

    @Test
    public void parseRole_normalizesAndDefaultsCorrectly() {
        // 1) Setup conditions (mock DocumentSnapshot return values)
        DocumentSnapshot docAdmin = mock(DocumentSnapshot.class);
        when(docAdmin.getString("role")).thenReturn("  admin  ");

        DocumentSnapshot docBarber = mock(DocumentSnapshot.class);
        when(docBarber.getString("role")).thenReturn("barber");

        DocumentSnapshot docMissing = mock(DocumentSnapshot.class);
        when(docMissing.getString("role")).thenReturn(null);

        DocumentSnapshot docInvalid = mock(DocumentSnapshot.class);
        when(docInvalid.getString("role")).thenReturn("manager");

        // 2) Call function under test
        String r1 = repo.parseRole(docAdmin);
        String r2 = repo.parseRole(docBarber);
        String r3 = repo.parseRole(docMissing);
        String r4 = repo.parseRole(docInvalid);

        // 3) Assertions
        assertEquals(UserRepository.ROLE_ADMIN, r1);
        assertEquals(UserRepository.ROLE_BARBER, r2);
        assertEquals(UserRepository.ROLE_CUSTOMER, r3);
        assertEquals(UserRepository.ROLE_CUSTOMER, r4);
    }
}
