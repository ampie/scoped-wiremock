package com.sbg.bdd.wiremock.scoped.client.junit;

import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScopedAdminHolder {
    private static Logger LOGGER =Logger.getLogger(ScopedAdminHolder.class.getName());
    private static ScopedAdmin scopedAdmin;
    private static GlobalCorrelationState globalCorrelationState;

    public static ScopedAdmin getScopedAdmin() {
        return scopedAdmin;
    }

    public static void setScopedAdmin(ScopedAdmin scopedAdmin) {
        if (ScopedAdminHolder.scopedAdmin != scopedAdmin) {
            if (ScopedAdminHolder.scopedAdmin != null && globalCorrelationState != null) {
                try {
                    ScopedAdminHolder.scopedAdmin.stopGlobalScope(globalCorrelationState);
                } catch (Exception e) {
                    LOGGER.warning(e.toString());
                    LOGGER.log(Level.FINE,"Could not stop global scope.", e);
                    e.printStackTrace();
                }
            }
            if (scopedAdmin != null) {
                try {
                    globalCorrelationState = scopedAdmin.startNewGlobalScope(new GlobalCorrelationState("unit-tests", new URL(((HasBaseUrl) scopedAdmin).baseUrl()), null, "unit"));
                } catch (Exception e) {
                    LOGGER.warning(e.toString());
                    e.printStackTrace();
                    LOGGER.log(Level.FINE,"Could not start global scope.", e);
                }
            }
            ScopedAdminHolder.scopedAdmin = scopedAdmin;
        }
    }

    public static GlobalCorrelationState getGlobalCorrelationState() {
        return globalCorrelationState;
    }
}
