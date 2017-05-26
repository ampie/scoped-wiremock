package com.sbg.bdd.wiremock.scoped.filter;

import java.io.IOException;

public class HttpCommandExecutor {
    public static HttpCommandExecutor INSTANCE = new HttpCommandExecutor();
    public String execute(HttpCommand command) throws IOException {
        return command.execute();
    }
}
