package com.sbg.bdd.wiremock.scoped.cdi;

import java.security.Principal;

public class ProxiablePrincipal implements PrincipalInterface2,Principal {
    @Override
    public String getName() {
        return "somename";
    }
}
