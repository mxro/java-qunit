package com.appjangle.qunit.tests;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;

import com.appjangle.qunit.internal.SslUtils;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;

public class TestSslPage {

    @Test
    public void test() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_24);

        SslUtils.allowInsecureSsl(webClient);
        SslUtils.disableSslCertificateValidation();

        webClient.getPage("https://appjangle.com/notyet");
    }

}
