package com.bc.calvalus.production;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProductionStatusTest {

    @Test
    public void testConstructors() {
        ProductionStatus unknown = ProductionStatus.UNKNOWN;
        assertEquals(ProductionState.UNKNOWN, unknown.getState());
        assertEquals("", unknown.getMessage());
        assertEquals(0.0f, unknown.getProgress(), 1e-5);

        ProductionStatus done = new ProductionStatus(ProductionState.COMPLETED, 1.0f);
        assertEquals(ProductionState.COMPLETED, done.getState());
        assertEquals("", done.getMessage());
        assertEquals(1.0f, done.getProgress(), 1e-5);

        ProductionStatus error = new ProductionStatus(ProductionState.ERROR, 0.0f, "File not found");
        assertEquals(ProductionState.ERROR, error.getState());
        assertEquals("File not found", error.getMessage());
        assertEquals(0.0f, error.getProgress(), 1e-5);

        try {
            new ProductionStatus(null, 0.0f, "File not found");
            fail("State must not be null");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            new ProductionStatus(ProductionState.COMPLETED, 0.0f, null);
            fail("State must not be null");
        } catch (NullPointerException e) {
            // ok
        }
    }


    @Test
    public void testAggregate() {
        assertEquals(null,
                     ProductionStatus.aggregate());
        assertEquals(new ProductionStatus(ProductionState.IN_PROGRESS, 0.4f, "Hello!"),
                     ProductionStatus.aggregate(new ProductionStatus(ProductionState.IN_PROGRESS, 0.4f, "Hello!")));
        assertEquals(new ProductionStatus(ProductionState.IN_PROGRESS, 0.3f, ""),
                     ProductionStatus.aggregate(new ProductionStatus(ProductionState.IN_PROGRESS, 0.2f, ""),
                                                new ProductionStatus(ProductionState.IN_PROGRESS, 0.4f, ""),
                                                new ProductionStatus(ProductionState.IN_PROGRESS, 0.3f, "")));
        assertEquals(new ProductionStatus(ProductionState.IN_PROGRESS, 0.8f, "Wait"),
                     ProductionStatus.aggregate(new ProductionStatus(ProductionState.IN_PROGRESS, 0.4f, "Wait"),
                                                new ProductionStatus(ProductionState.COMPLETED, 1.0f, ""),
                                                new ProductionStatus(ProductionState.COMPLETED, 1.0f, "")));
        assertEquals(new ProductionStatus(ProductionState.COMPLETED, 1.0f, "This was hard"),
                     ProductionStatus.aggregate(new ProductionStatus(ProductionState.COMPLETED, 1.0f, ""),
                                                new ProductionStatus(ProductionState.COMPLETED, 1.0f, "This was hard"),
                                                new ProductionStatus(ProductionState.COMPLETED, 1.0f, "")));
        assertEquals(new ProductionStatus(ProductionState.ERROR, 0.8f, "I/O problem"),
                     ProductionStatus.aggregate(new ProductionStatus(ProductionState.COMPLETED, 1.0f, ""),
                                                new ProductionStatus(ProductionState.ERROR, 0.4f, "I/O problem"),
                                                new ProductionStatus(ProductionState.COMPLETED, 1.0f, "")));
        assertEquals(new ProductionStatus(ProductionState.CANCELLED, 0.3f, "Go away"),
                     ProductionStatus.aggregate(new ProductionStatus(ProductionState.IN_PROGRESS, 0.2f, ""),
                                                new ProductionStatus(ProductionState.IN_PROGRESS, 0.6f, ""),
                                                new ProductionStatus(ProductionState.CANCELLED, 0.1f, "Go away")));
    }

    @Test
    public void testIsDone() {
        assertEquals(true, new ProductionStatus(ProductionState.COMPLETED).isDone());
        assertEquals(true, new ProductionStatus(ProductionState.ERROR).isDone());
        assertEquals(true, new ProductionStatus(ProductionState.CANCELLED).isDone());

        assertEquals(false, new ProductionStatus(ProductionState.UNKNOWN).isDone());
        assertEquals(false, new ProductionStatus(ProductionState.IN_PROGRESS).isDone());
        assertEquals(false, new ProductionStatus(ProductionState.WAITING).isDone());
    }

    @Test
    public void testEquals() {
        ProductionStatus status = ProductionStatus.UNKNOWN;
        assertTrue(status.equals(status));
        assertTrue(status.equals(new ProductionStatus(ProductionState.UNKNOWN)));
        assertTrue(new ProductionStatus(ProductionState.COMPLETED).equals(
                new ProductionStatus(ProductionState.COMPLETED)));
        assertTrue(new ProductionStatus(ProductionState.IN_PROGRESS, 0.2f, "Dabei!").equals(
                new ProductionStatus(ProductionState.IN_PROGRESS, 0.2f, "Dabei!")));
        assertTrue(new ProductionStatus(ProductionState.IN_PROGRESS, 0.20001f, "Dabei!").equals(
                new ProductionStatus(ProductionState.IN_PROGRESS, 0.20003f, "Dabei!")));
    }

    @Test
    public void testNotEquals() {
        assertFalse(new ProductionStatus(ProductionState.ERROR, 0.1f, "").equals(
                new ProductionStatus(ProductionState.COMPLETED, 0.1f, "")));
        assertFalse(new ProductionStatus(ProductionState.IN_PROGRESS, 0.21f, "").equals(
                new ProductionStatus(ProductionState.IN_PROGRESS, 0.22f, "")));
        assertFalse(new ProductionStatus(ProductionState.IN_PROGRESS, 0.2f, "Dabei!").equals(
                new ProductionStatus(ProductionState.IN_PROGRESS, 0.2f, "Vorbei!")));
    }
}
