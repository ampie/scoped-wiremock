package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.sbg.bdd.wiremock.scoped.admin.ScopeExtensionsOnClient;


public class ScopeExtensions extends ScopeExtensionsOnClient implements AdminApiExtension{
    @Override
    public void contributeAdminApiRoutes(Router router) {
        currentAdmin=new CorrelatedScopeAdmin();
        super.contributeAdminApiRoutes(router);
    }

}
