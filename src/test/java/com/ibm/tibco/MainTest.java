package com.ibm.tibco;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void shouldCreateMainInstance() {
        TableComparatorApplication main = new TableComparatorApplication();
        assertNotNull(main);
    }
}