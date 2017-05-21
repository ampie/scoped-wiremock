package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.ampie.wiremock.CorrelatedScopeAdmin;


public class ScopeExtensions extends ScopeExtensionsOnClient implements AdminApiExtension{
    @Override
    public void contributeAdminApiRoutes(Router router) {
        currentAdmin=new CorrelatedScopeAdmin();
        super.contributeAdminApiRoutes(router);
    }

}
