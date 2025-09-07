package com.repeatquran.history;

import org.junit.Test;

import static org.junit.Assert.*;

public class HistoryViewModelTest {

    @Test
    public void instantiatesAndReturnsZeroByDefault() {
        HistoryViewModel vm = new HistoryViewModel();
        assertNotNull(vm);
        assertEquals(0, vm.getLastSessionsCount());
    }
}

