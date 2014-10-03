package com.appjangle.qunit.tests;

import org.junit.Test;

import com.appjangle.qunit.QUnitHtmlUnit;

public class TestQUnitEmbedding {

	@Test
	public void test_simple_tests() {
		QUnitHtmlUnit
				.runQUnitTest("file:///"+getClass().getResource("/qunit_example.html").getFile());
	}

}
