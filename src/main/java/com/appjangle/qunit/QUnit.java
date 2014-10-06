package com.appjangle.qunit;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import junit.framework.Assert;
import be.roam.hue.doj.Doj;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class QUnit {

    public static void run(final Object context, final String resourcePath) {
        run("file:///" + context.getClass().getResource("/" + resourcePath).getFile());
    }

    /**
     * Will load the specified HTML page, run QUnit tests and throw JUnit
     * assertion errors if any of the QUnit test cases failed.
     * 
     * @param pageUrl
     */
    public static void run(final String pageUrl) {

        try {
            WebClient webClient = null;
            try {
                System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "error");
                webClient = new WebClient(BrowserVersion.FIREFOX_24);

                /*
                 * Temporarily required since Godaddy certificates are not added
                 * to Java correctly.
                 */
                disableSslCertificateValidation();
                webClient.getOptions().setUseInsecureSSL(true);

                webClient.getOptions().setTimeout(120 * 1000);

                final HtmlPage page = webClient.getPage(pageUrl);

                Thread.sleep(1000);

                int retriesLeft = 480;
                while (!isTestComplete(page)) {
                    Thread.sleep(1000);
                    retriesLeft--;
                    if (retriesLeft == 0) {
                        Assert.fail("Tests took longer than 480 s to execute.");
                    }
                }

                final HtmlElement element = page.getHtmlElementById("qunit");

                if (element.getTextContent().indexOf("0 tests of 0") != -1) {
                    Assert.fail("No QUnit tests ran for [" + pageUrl + "]");
                }

                for (final HtmlElement liElement : Doj.on(element).get("li").allElements()) {
                    if (liElement.getAttribute("class").equals("fail")) {
                        for (final HtmlElement spanElement : Doj.on(liElement).get("span").allElements()) {
                            if (spanElement.getAttribute("class").equals("test-name")) {

                                for (final HtmlElement embeddedLi : Doj.on(liElement).get("ol li").allElements()) {

                                    if (embeddedLi.getAttribute("class").equals("fail")) {

                                        final Doj testMessage = Doj.on(embeddedLi).get("span");

                                        // final HtmlElement source = Doj.on(
                                        // embeddedLi).allElements()[1];

                                        Assert.fail("QUnit test failed: " + spanElement.asText() + "\nAssertion: "
                                                + testMessage.firstElement().asText() + "\nSource:\n"
                                                + embeddedLi.asText());
                                    }
                                }

                                Assert.fail("QUnit test failed: " + spanElement.asText());

                            }
                            Assert.fail("QUnit test failed. Could not determine name.");
                        }
                    }
                }

            } finally {
                if (webClient != null) {

                    webClient.closeAllWindows();
                }

            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }

    }

    public static boolean isTestComplete(final HtmlPage page) {
        final DomElement testResult = page.getElementById("qunit-testresult");

        return testResult.asText().contains("Tests completed");
    }

    private final static void disableSslCertificateValidation() {

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
