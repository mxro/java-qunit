package com.appjangle.qunit.tests;

import org.junit.Test;

import com.appjangle.qunit.QUnitHtmlUnit;

public class TestQUnitEmbedding {

	@Test(expected = RuntimeException.class)
	public void test_simple_tests() {
		QUnitHtmlUnit
				.runQUnitTest("file:///M:/Git/qunit-htmlunit-java/src/main/resources/qunit_example.html");
	}

}
