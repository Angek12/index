package com.index.platform;

import static org.junit.jupiter.api.Assertions.*;

import com.index.platform.exception.ApiException;
import com.index.platform.service.LoginGuard;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LoginGuardTest {

    @Test
    void blocksAfterFiveFailures() {
        LoginGuard g = new LoginGuard();
        for (int i = 0; i < LoginGuard.MAX_FAILURES; i++) {
            g.check("a@test.rw");
            g.failure("a@test.rw");
        }
        ApiException e = assertThrows(ApiException.class, () -> g.check("a@test.rw"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
    }

    @Test
    void otherAccountsAreNotAffected() {
        LoginGuard g = new LoginGuard();
        for (int i = 0; i < LoginGuard.MAX_FAILURES; i++) g.failure("a@test.rw");
        assertDoesNotThrow(() -> g.check("b@test.rw"));
    }

    @Test
    void successClearsTheFailures() {
        LoginGuard g = new LoginGuard();
        for (int i = 0; i < LoginGuard.MAX_FAILURES - 1; i++) g.failure("a@test.rw");
        g.success("a@test.rw");
        g.failure("a@test.rw");
        assertDoesNotThrow(() -> g.check("a@test.rw"));
    }
}
