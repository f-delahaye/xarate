package io.xarate.protocols.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;

public class StompRunner {
    static final org.slf4j.Logger logger = LoggerFactory.getLogger(StompRunner.class);

    static StompNettyServer server;

    @BeforeAll
    static void beforeAll() {
        server = new StompNettyServer(0, "/stomp");
        server.run();
        int port = server.getPort();
        System.setProperty("karate.server.port", port + "");
    }

    @AfterAll
    static void afterAll() {
        server.close();
    }

    @Test
    void testStomp() {
        Results results = Runner
                .path("classpath:io/xarate/protocols/stomp/stomp.feature")
                .configDir("classpath:io/xarate/protocols/stomp")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}