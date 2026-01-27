package com.example.barber4u.adapters;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.example.barber4u.models.Appointment;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

public class removeByIdTest {

    private BarberAppointmentsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        // 1) Setup conditions
        adapter = spy(constructAdapterWithMocks());

        // IMPORTANT: Prevent RecyclerView internals from running in unit test
        // (this avoids the mObservers null crash)
        doNothing().when(adapter).notifyItemRemoved(anyInt());

        // Seed adapter internal items list
        List<Appointment> items = getItemsList(adapter);

        Appointment a1 = mock(Appointment.class);
        when(a1.getId()).thenReturn("1");

        Appointment a2 = mock(Appointment.class);
        when(a2.getId()).thenReturn("2");

        items.add(a1);
        items.add(a2);

        assertEquals(2, items.size());
    }

    @Test
    public void removeById_removesCorrectItem_andCallsNotifyItemRemoved() throws Exception {
        // 2) Call function under test
        adapter.removeById("1");

        // 3) Assertions
        List<Appointment> items = getItemsList(adapter);
        assertEquals(1, items.size());

        verify(adapter).notifyItemRemoved(0);
    }

    // ---- Helpers ----

    private static BarberAppointmentsAdapter constructAdapterWithMocks() throws Exception {
        Constructor<?>[] ctors = BarberAppointmentsAdapter.class.getDeclaredConstructors();
        if (ctors.length == 0) throw new IllegalStateException("No constructor found");

        Constructor<?> ctor = ctors[0];
        ctor.setAccessible(true);

        Class<?>[] paramTypes = ctor.getParameterTypes();
        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> t = paramTypes[i];
            if (t.isPrimitive()) args[i] = defaultPrimitiveValue(t);
            else args[i] = mock(t);
        }

        return (BarberAppointmentsAdapter) ctor.newInstance(args);
    }

    @SuppressWarnings("unchecked")
    private static List<Appointment> getItemsList(BarberAppointmentsAdapter adapter) throws Exception {
        Field f = BarberAppointmentsAdapter.class.getDeclaredField("items");
        f.setAccessible(true);
        return (List<Appointment>) f.get(adapter);
    }

    private static Object defaultPrimitiveValue(Class<?> t) {
        if (t == boolean.class) return false;
        if (t == byte.class) return (byte) 0;
        if (t == short.class) return (short) 0;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == float.class) return 0f;
        if (t == double.class) return 0d;
        if (t == char.class) return '\0';
        return 0;
    }
}
