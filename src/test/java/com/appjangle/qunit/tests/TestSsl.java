package com.appjangle.qunit.tests;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class TestSsl {

    @Test
    public void test() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_24);

        final HtmlPage page = webClient.getPage("https://appjangle.com/notyet");
    }

}
