package com.appjangle.qunit;

import junit.framework.Assert;
import be.roam.hue.doj.Doj;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class QUnitHtmlUnit {

	/**
	 * Will load the specified HTML page, run QUnit tests and throw JUnit
	 * assertion errors if any of the QUnit test cases failed.
	 * 
	 * @param pageUrl
	 */
	public static void runQUnitTest(final String pageUrl) {
		try {
			WebClient webClient = null;
			try {
				webClient = new WebClient();

				webClient.setTimeout(1000 * 60 * 10);
				final HtmlPage page = webClient.getPage(pageUrl);

				Thread.sleep(5000);

				final HtmlElement element = page.getHtmlElementById("qunit");

				if (element.getTextContent().indexOf("0 tests of 0") != -1) {
					Assert.fail("No QUnit tests ran for [" + pageUrl + "]");
				}

				for (final HtmlElement elem : Doj.on(element).get("li")
						.allElements()) {
					if (elem.getAttribute("class").equals("fail")) {
						for (final HtmlElement testName : Doj.on(elem)
								.get("span").allElements()) {
							if (testName.getAttribute("class").equals(
									"test-name")) {
								Assert.fail("QUnit test failed: "
										+ testName.asText());
							}
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
}
