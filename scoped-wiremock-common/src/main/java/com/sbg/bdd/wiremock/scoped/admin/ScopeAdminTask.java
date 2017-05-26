package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.AdminTask;

public abstract class ScopeAdminTask  implements AdminTask{
    protected ScopedAdmin admin;

    public ScopeAdminTask(ScopedAdmin scopedAdmin) {
        this.admin = scopedAdmin;
    }
}
