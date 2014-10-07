qunit-htmlunit-java
===================

[![Build Status](https://travis-ci.org/mxro/qunit-htmlunit-java.svg?branch=master)](https://travis-ci.org/mxro/qunit-htmlunit-java)

Java and JavaScript Utilities to embed [QUnit](http://qunitjs.com/) tests in JUnit tests via HTMLUnit.

These utilities used to test the [Nextweb JavaScript API](http://nextweb.io) implementation provided by [Appjangle](http://appjangle.com) 
using [opsunit](https://github.com/mxro/opsunit).

## Usage

Link the maven dependency:

    <dependency>
		<groupId>com.appjangle.qunit</groupId>
		<artifactId>java-qunit</artifactId>
		<version>[0.0.3,)</version>
    <dependency>

Add maven repository:

    <repository>
        <id>onedb Releases</id>
        <url>http://dl.dropbox.com/u/957046/onedb/mvn-releases</url>
    </repository>

Create a new HTML page with QUnit tests ([see example](https://github.com/mxro/qunit-htmlunit-java/blob/master/src/main/resources/qunit_example.html)) in `src/main/resources`.

Create a new JUnit test in `src/main/test` and add this statement to the test:

   QUnit.run(this, "qunit_example.html");
   
(see [TestQUnitEmbedding.java](https://github.com/mxro/qunit-htmlunit-java/blob/master/src/test/java/com/appjangle/qunit/tests/TestQUnitEmbedding.java))

If any of the QUnit tests fail, the respective JUnit test case will fail as well.

## Related

- (Doj)[http://code.google.com/p/hue/wiki/Doj]
- [Continuous Integration for QUnit tests](http://www.bitmechanic.com/2009/01/09/continuous-integration-for-qunit-tests.html)
- [qunit-to-junit-for-phantom.js](https://gist.github.com/1363104)
- [qunit-maven-plugin](https://bitbucket.org/sebr/qunit-maven-plugin)
- [Continuous Integration for JavaScript using Bamboo](http://blogs.atlassian.com/2010/01/continuous_integration_javascript_jquery_qunit/)
- [AntXmlTestResultsParser](https://confluence.atlassian.com/download/attachments/289277357/AntXmlTestResultsParser.java?version=1&modificationDate=1197607895622&api=v2)