package com.appjangle.qunit.tests;

import com.appjangle.qunit.QUnitHtmlUnit;

public class TestQUnitEmbedding {

	public void test_simple_tests() {
		QUnitHtmlUnit
				.runQUnitTest(getClass().getResource("/qunit_example.html").getFile());
	}

}
