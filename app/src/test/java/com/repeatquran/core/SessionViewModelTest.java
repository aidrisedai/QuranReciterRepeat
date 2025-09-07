package com.repeatquran.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class SessionViewModelTest {

    @Test
    public void instantiatesAndDefaultsInactive() {
        SessionViewModel vm = new SessionViewModel();
        assertNotNull(vm);
        assertFalse(vm.hasActiveSession());
    }
}

