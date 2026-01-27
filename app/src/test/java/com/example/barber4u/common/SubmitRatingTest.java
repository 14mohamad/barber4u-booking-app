package com.example.barber4u.common;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class SubmitRatingTest {

    private MessagesFragment fragment;            // spy
    private FirebaseFirestore db;                 // mock
    private WriteBatch batch;                     // mock

    // Firestore chain mocks
    private CollectionReference apptsCol;
    private DocumentReference apptDoc;

    private CollectionReference usersCol;
    private DocumentReference userDoc;
    private CollectionReference messagesCol;
    private DocumentReference msgDoc;

    @Before
    public void setUp() throws Exception {
        // ---- 1) Setup conditions ----
        fragment = spy(new MessagesFragment());

        // Avoid Toast/requireContext inside success/failure callbacks in a JVM unit test
        // (submitRating checks isAdded() before showing Toast)
        doReturn(false).when(fragment).isAdded();

        db = mock(FirebaseFirestore.class);
        batch = mock(WriteBatch.class);

        apptsCol = mock(CollectionReference.class);
        apptDoc = mock(DocumentReference.class);

        usersCol = mock(CollectionReference.class);
        userDoc = mock(DocumentReference.class);
        messagesCol = mock(CollectionReference.class);
        msgDoc = mock(DocumentReference.class);

        // Inject mocked db into fragment (db is a private field)
        Field dbField = MessagesFragment.class.getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(fragment, db);

        when(db.batch()).thenReturn(batch);

        // appointments/{appointmentId}
        when(db.collection("appointments")).thenReturn(apptsCol);
        when(apptsCol.document(anyString())).thenReturn(apptDoc);

        // users/{uid}/messages/{msgId}
        when(db.collection("users")).thenReturn(usersCol);
        when(usersCol.document(anyString())).thenReturn(userDoc);
        when(userDoc.collection("messages")).thenReturn(messagesCol);
        when(messagesCol.document(anyString())).thenReturn(msgDoc);

        // batch.commit() returns Task<Void>, and submitRating chains listeners on it
        @SuppressWarnings("unchecked")
        Task<Void> task = mock(Task.class);
        when(batch.commit()).thenReturn(task);
        when(task.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(task);
        when(task.addOnFailureListener(any(OnFailureListener.class))).thenReturn(task);
    }

    @Test
    public void submitRating_updatesAppointment_deletesMessage_andCommitsBatch() throws Exception {
        // ---- 1) Setup conditions ----
        MessageItem msg = new MessageItem(
                "msg123",
                "text",
                "appt999",
                "barber42",
                "Barber Name"
        );
        int rating = 4;

        // FirebaseAuth.getInstance().getUid() is called inside submitRating.
        // We mock it to return a known uid.
        FirebaseAuth authMock = mock(FirebaseAuth.class);
        when(authMock.getUid()).thenReturn("user123");

        try (MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(authMock);

            // ---- 2) Call function under test (private method via reflection) ----
            Method m = MessagesFragment.class.getDeclaredMethod(
                    "submitRating",
                    MessageItem.class,
                    int.class
            );
            m.setAccessible(true);
            m.invoke(fragment, msg, rating);
        }

        // ---- 3) Assertions ----

        // verify correct appointment doc used
        verify(apptsCol).document("appt999");
        verify(batch).update(eq(apptDoc), anyMap());

        // capture the map passed to batch.update and assert rating key/value exists
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(batch).update(eq(apptDoc), mapCaptor.capture());
        Map<String, Object> updateMap = mapCaptor.getValue();

        assertEquals("rating must be set", rating, updateMap.get("rating"));
        assertTrue("ratedAt must exist", updateMap.containsKey("ratedAt"));
        assertNotNull("ratedAt value must not be null", updateMap.get("ratedAt"));
        // (We don't compare exact FieldValue object; just ensure it is present.)

        // verify correct message doc deleted
        verify(usersCol).document("user123");
        verify(messagesCol).document("msg123");
        verify(batch).delete(msgDoc);

        // verify commit called and listeners attached
        verify(batch).commit();
    }
}
