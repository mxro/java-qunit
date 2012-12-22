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

				int retriesLeft = 120;
				while (!page.asText().contains("Tests completed")) {
					Thread.sleep(1000);
					retriesLeft--;
					if (retriesLeft == 0) {
						Assert.fail("Tests took longer than 120 s to execute.");
					}
				}

				final HtmlElement element = page.getHtmlElementById("qunit");

				if (element.getTextContent().indexOf("0 tests of 0") != -1) {
					Assert.fail("No QUnit tests ran for [" + pageUrl + "]");
				}

				for (final HtmlElement liElement : Doj.on(element).get("li")
						.allElements()) {
					if (liElement.getAttribute("class").equals("fail")) {
						for (final HtmlElement spanElement : Doj.on(liElement)
								.get("span").allElements()) {
							if (spanElement.getAttribute("class").equals(
									"test-name")) {

								for (final HtmlElement embeddedLi : Doj
										.on(liElement).get("ol li")
										.allElements()) {

									if (embeddedLi.getAttribute("class")
											.equals("fail")) {

										final Doj testMessage = Doj.on(
												embeddedLi).get("span");

										// final HtmlElement source = Doj.on(
										// embeddedLi).allElements()[1];

										// System.out.println(source.size());
										Assert.fail("QUnit test failed: "
												+ spanElement.asText()
												+ "\nAssertion: "
												+ testMessage.firstElement()
														.asText()
												+ "\nSource:\n"
												+ embeddedLi.asText());
									}
								}

								Assert.fail("QUnit test failed: "
										+ spanElement.asText());

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
}
