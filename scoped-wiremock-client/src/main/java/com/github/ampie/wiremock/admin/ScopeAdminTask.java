package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.AdminTask;

public abstract class ScopeAdminTask  implements AdminTask{
    protected ScopedAdmin admin;

    public ScopeAdminTask(ScopedAdmin scopedAdmin) {
        this.admin = scopedAdmin;
    }
}
