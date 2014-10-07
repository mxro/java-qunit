package com.appjangle.qunit.tests;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;

import com.appjangle.qunit.QUnit;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

public class TestSslScript {

    @Test
    public void test() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        QUnit.run(this, "qunit_ssl.html");
    }

}
