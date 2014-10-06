package com.appjangle.qunit.internal;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SslUtils {

    public final static void disableSslCertificateValidation() {
    
        final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
    
            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                // nothing
            }
    
            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                // nothing
            }
        } };
    
        final HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                // always true
                return true;
            }
        };
    
        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (final Exception e) {
        }
    }

}
