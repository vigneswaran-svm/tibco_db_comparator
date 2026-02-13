package com.ibm.tibco;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void shouldCreateMainInstance() {
        Main main = new Main();
        assertNotNull(main);
    }
}