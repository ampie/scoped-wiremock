package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.RequestSpec;
import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.google.common.collect.ImmutableBiMap;
import com.sbg.bdd.wiremock.scoped.common.Reflection;

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
        //Resource management
        router.add(POST, "/resources/list", new ListResourcesTask(currentAdmin));
        router.add(POST, "/resources/read", new ReadResourceTask(currentAdmin));
        router.add(POST, "/resources/write", new WriteResourceTask(currentAdmin));
        //Scope management
        router.add(POST, "/global_scopes/new", new StartNewGlobalScopeTask(currentAdmin));
        router.add(POST, "/global_scopes/stop",new StopGlobalScopeTask(currentAdmin));
        router.add(POST, "/scopes/stop",new StopNestedScopeTask(currentAdmin));
        router.add(POST, "/scopes/join", new JoinKnownCorrelatedScopeTask(currentAdmin));
        router.add(POST, "/scopes/sync", new SyncCorrelatedScopeTask(currentAdmin));
        router.add(POST, "/scopes/get",new  GetCorrelatedScopeTask(currentAdmin));
        router.add(POST, "/user_scopes/join", new JoinUserScopeTask(currentAdmin));
        //Step management
        router.add(POST, "/scopes/steps/start",new  StartStepTask(currentAdmin));
        router.add(POST, "/scopes/steps/stop",new StopStepTask(currentAdmin));
        router.add(POST, "/scopes/steps/find_exchanges",new FindExchangesAgainstStepTask(currentAdmin));
        //Others
        router.add(POST, "/scopes/mappings/find",new GetMappingsInScopeTask(currentAdmin));
        router.add(POST, "/scopes/exchanges/find",new  FindExchangesInScopeTask(currentAdmin));
        router.add(POST, "/scopes/exchanges/journal",new JournalTask(currentAdmin));
        router.add(POST, "/extended_mappings",new CreateExtendedStubMappingTask(currentAdmin));
        router.add(POST, "/extended_count",new CountByExtendedRequestPatternTask(currentAdmin));
    }


    public static  ScopedAdmin getCurrentAdmin() {
        return currentAdmin;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
