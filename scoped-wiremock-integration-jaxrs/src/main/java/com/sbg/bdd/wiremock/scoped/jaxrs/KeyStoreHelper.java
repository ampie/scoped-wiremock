package com.sbg.bdd.wiremock.scoped.jaxrs;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class KeyStoreHelper {

    public static final String SYSPROP_KEYSTORE = "javax.net.ssl.keyStore";
    public static final String SYSPROP_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

    private static final Logger LOGGER = Logger.getLogger(KeyStoreHelper.class.getName());

    private KeyStore keystore = null;

    @PostConstruct
    public void initialise() {
        setKeystore(createKeystoreFromFile());
    }

    private KeyStore createKeystoreFromFile() {
        String keystorePath = System.getProperty(SYSPROP_KEYSTORE);
        if (keystorePath == null || keystorePath.trim().length() == 0) {
            LOGGER.log(Level.SEVERE, "ERROR: No keystore specified in system properties");
            return null;
        }
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            InputStream keyStream = new FileInputStream(keystorePath);
            ks.load(keyStream, getKeystorePassword().toCharArray());
            keyStream.close();
            return ks;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            LOGGER.log(Level.SEVERE, "ERROR creating keystore from system properties", e);
            return null;
        }
    }

    public String getKeystorePassword() {
        return System.getProperty(SYSPROP_KEYSTORE_PASSWORD, "");
    }

    public KeyStore getKeystore() {
        return keystore;
    }

    private void setKeystore(KeyStore keystore) {
        this.keystore = keystore;
    }

}
