package com.sbg.bdd.wiremock.scoped.integration;

import java.io.IOException;

/**
 * Only to make mocking easier during unit tests
 */
public class HttpCommandExecutor {
    public static HttpCommandExecutor INSTANCE = new HttpCommandExecutor();
    public String execute(HttpCommand command) throws IOException {
        return command.execute();
    }
}
