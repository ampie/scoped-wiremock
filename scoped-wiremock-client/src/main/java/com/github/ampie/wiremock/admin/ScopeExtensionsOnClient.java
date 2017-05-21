package com.github.ampie.wiremock.admin;

import com.github.ampie.wiremock.common.Reflection;
import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.RequestSpec;
import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.google.common.collect.ImmutableBiMap;

import java.util.Map;

import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;


public class ScopeExtensionsOnClient implements AdminApiExtension{
    static protected ScopedAdmin currentAdmin;
    @Override
    public void contributeAdminApiRoutes(Router router) {
        ImmutableBiMap.Builder<RequestSpec, AdminTask> builder = Reflection.getValue(router,"builder");
        Map.Entry<RequestSpec,AdminTask>[] entries= Reflection.getValue(builder,"entries");
        for (Map.Entry<RequestSpec, AdminTask> entry : entries) {
            if( entry !=null && (entry.getValue() instanceof com.github.tomakehurst.wiremock.admin.tasks.CreateStubMappingTask)){
                System.out.println("Overriding " + entry.getValue().getClass());
                Reflection.setValue(entry,"value",new CreateStubMappingTask());
            }
        }
        //Scope management
        router.add(POST, "/scopes/join", new JoinKnownCorrelatedScopeTask(currentAdmin));
        router.add(POST, "/scopes/new", new StartNewCorrelatedScopeTask(currentAdmin));
        router.add(POST, "/scopes/sync", new SyncCorrelatedScopeTask(currentAdmin));
        router.add(POST, "/scopes/stop",new  StopCorrelatedScopeTask(currentAdmin));
        router.add(POST, "/scopes/get",new  GetCorrelatedScopeTask(currentAdmin));
        //Step management
        router.add(POST, "/scopes/steps/start",new  StartStepTask(currentAdmin));
        router.add(POST, "/scopes/steps/stop",new StopStepTask(currentAdmin));
        router.add(POST, "/scopes/steps/find_exchanges",new FindExchangesAgainstStepTask(currentAdmin));
        //Others
        router.add(POST, "/scopes/mappings/find",new GetMappingsInScopeTask(currentAdmin));
        router.add(POST, "/scopes/exchanges/find",new  FindExchangesInScopeTask(currentAdmin));
    }


    public static  ScopedAdmin getCurrentAdmin() {
        return currentAdmin;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
