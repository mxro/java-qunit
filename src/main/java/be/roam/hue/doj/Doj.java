/*
 * Copyright 2009 Roam - roam.be
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.roam.hue.doj;

import static be.roam.hue.doj.MatchType.EQUALS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;

/**
 * Doj is a jQuery-style DOM traversal tool, to be used in conjunction with
 * HtmlUnit's {@link DomElement}.
 * <p>
 * The best examples of usage can be found in the unit tests for this class, but
 * here are some pointers.
 * </p>
 * <p>
 * Say you want to get value of the text input inside a form with class
 * "search", located inside a div with id sidebar. Here's how you can do it with
 * Doj:
 * </p>
 * 
 * <pre>
 * <code>
 * // Possibility #1 - plain and simple
 * String value1 = Doj.on(page).getById("sidebar").get("form").withClass("search").get("input").value();
 * // Possibility #2 - concise with simple CSS selectors
 * String value2 = Doj.on(page).get("#sidebar form.search input").value();
 * </code>
 * </pre>
 * <p>
 * A Doj instance is <strong>immutable</strong>. In the above code, each
 * possibility creates 5 new Doj instances:
 * </p>
 * <ol>
 * <li>Doj.on(): the initial object</li>
 * <li>One for getting an element by id "sidebar"</li>
 * <li>One for getting descendants by tag name "form"</li>
 * <li>One for filtering the context elements by class "search"</li>
 * <li>One for getting descendants by tag name "input"</li>
 * </ol>
 * <p>
 * Because Doj relies on two implementations to keep the DOM traversal code
 * clean (one for an empty Doj, one for a non-empty Doj), construction of a new
 * Doj instance is handled by the <code>on(...)</code> factory methods.
 * </p>
 * <p>
 * Note that Doj implements {@link Iterable}, allowing you to loop over the
 * context elements with a for-each loop. Following code will print the value of
 * the class attribute of each div on the page:
 * </p>
 * 
 * <pre>
 * <code>
 * Doj allDivsOnThePage = Doj.on(page).get("div");
 * for (Doj div : allDivsOnThePage) {
 *      System.out.println(div.attribute("class"));
 * }
 * </code>
 * </pre>
 * <p>
 * When traversing the DOM, missing elements will not throw exceptions. Assume
 * you've got a page without any tables and without any elements with the class
 * "nono":
 * </p>
 * 
 * <pre>
 * <code>
 * String thisWillBeNull = Doj.on(page).get("table").get("a").get("span").attribute("title");
 * String asWillThis = Doj.on(page).get(".nono").get("a").get("span").attribute("title");
 * </code>
 * </pre>
 * <p>
 * Checking whether there is actually anything there, goes like this:
 * </p>
 * 
 * <pre>
 * <code>
 * boolean thisIsTrue = Doj.on(page).get("table").get("a").get("span").isEmpty();
 * boolean thisToo = Doj.on(page).get(".nono").get("a").get("span").size() == 0;
 * boolean andThis = Doj.on(page).get("table").isEmpty();
 * boolean thisIsProbablyFalse = Doj.on(page).get("body").isEmpty();
 * </code>
 * </pre>
 * 
 * @author Kevin Wetzels
 */
public abstract class Doj implements Iterable<Doj> {

    public static final Doj EMPTY = new EmptyDoj();

    /**
     * Gets the wrapped element at the given index.
     * <p>
     * When no such element exists, an empty Doj instance is returned.
     * </p>
     * 
     * @param index
     *            index of the element to retrieve - pass a negative value to
     *            start from the back
     * @return new Doj instance
     */
    public abstract Doj get(int index);

    /**
     * Gets the element at the given index.
     * 
     * @param index
     *            index of the element to retrieve - pass a negative value to
     *            start from the back
     * @return the element at the given index, or null if no such element exists
     */
    public abstract DomElement getElement(int index);

    /**
     * Creates a new Doj instance by removing the element at the given index
     * from the context.
     * <p>
     * If no such element exists, the current instance is returned.
     * </p>
     * 
     * @param index
     *            index of the element to remove - pass a negative value to
     *            start from the back
     * @return new Doj instance
     */
    public abstract Doj remove(int index);

    /**
     * Merges the Doj instance with the current instance to create a new Doj
     * instance containing the context elements of both.
     * 
     * @param doj
     *            doj to merge with this one
     * @return new Doj instance
     */
    public Doj merge(final Doj doj) {
        final List<DomElement> result = new ArrayList<DomElement>();
        for (final DomElement element : allElements()) {
            result.add(element);
        }
        for (final DomElement element : doj.allElements()) {
            result.add(element);
        }
        return on(result);
    }

    /**
     * Creates a new Doj instance consisting of the children of the context that
     * have the given id.
     * <p>
     * <strong>Note:</strong> due to HtmlUnit's implementation of
     * {@link DomElement#getElementById(java.lang.String)}, this will not look
     * for the element with the given id as a descendant of the context
     * elements, but for the element with the given id. For instance:
     * <code>Doj.on(page).getById("header").getById("header").size()</code> will
     * not return 0 as you would expect, but 1.
     * </p>
     * 
     * @param id
     *            id to match
     * @return new Doj instance
     */
    public abstract Doj getById(String id);

    /**
     * Creates a new Doj instance containing the next sibling elements of the
     * current context elements.
     * 
     * @return new Doj instance
     */
    public abstract Doj next();

    /**
     * Creates a new Doj instance containing the next sibling elements of the
     * current context elements, matching the given tag.
     * <p>
     * Unlike {@link #next()}, this method will keep looking for the first
     * matching sibling until it finds a match or is out of siblings.
     * </p>
     * 
     * @param tag
     *            tag to match
     * @return new Doj instance
     */
    public abstract Doj next(String tag);

    /**
     * Creates a new Doj instance containing the previous sibling elements of
     * the current context elements.
     * 
     * @return new Doj instance
     */
    public abstract Doj previous();

    /**
     * Creates a new Doj instance containing the previous sibling elements of
     * the current context elements, matching the given tag.
     * <p>
     * Unlike {@link #previous()}, this method will keep looking for the first
     * matching sibling until it finds a match or is out of siblings.
     * </p>
     * 
     * @param tag
     *            tag to match
     * @return new Doj instance
     */
    public abstract Doj previous(String tag);

    /**
     * Creates a new Doj instance containing the direct parent elements of the
     * current context elements.
     * 
     * @return new Doj instance
     */
    public abstract Doj parent();

    /**
     * Creates a new Doj instance containing the parent elements of the current
     * context elements that match the given tag.
     * <p>
     * Unlike {@link #parent() }, this method will keep traversing up the DOM
     * until a match is found or the top of the DOM has been found
     * </p>
     * 
     * @param tag
     *            tag to match
     * @return new Doj instance
     */
    public abstract Doj parent(String tag);

    /**
     * Creates a new Doj instance without the duplicate elements from the
     * original.
     * <p>
     * Calling this method should not be necessary since Doj tries to use it
     * where possible.
     * </p>
     * <p>
     * Note that in order to function correctly, this method will need to set
     * some data on the element to serve as a hash code - which is missing in
     * HtmlUnit's {@link DomElement} - but only if there's no id to use.
     * </p>
     * 
     * @return new Doj instance
     */
    public abstract Doj unique();

    /**
     * Creates a new Doj instance containing the elements matching the given
     * (simple) CSS selector.
     * <p>
     * Following selectors are allowed:
     * </p>
     * <dl>
     * <dt>type selectors</dt>
     * <dd>HTML element tag names, e.g. "h1" will only match h1 elements</dd>
     * <dt>class selectors</dt>
     * <dd>passing ".something" will only match the elements with the class
     * "something"</dd>
     * <dt>id selectors</dt>
     * <dd>pass "#theid" to math the element with id "theid"</dd>
     * <dt>a combination of the above selectors</dt>
     * <dd>passing "div.article" wil only match the div elements with class
     * "article"</dd>
     * <dt>selectors with descendant combinators</dt>
     * <dd>passing "div.article a.more" will only match the anchors with class
     * "more" that are descendants of a div with class "article". But when using
     * a selector such as "div.article p#someid", remember that HtmlUnit will
     * look for the element with the given id anywhere on the page, not just
     * within divs with class "article".</dd>
     * <dt>grouped selectors</dt>
     * <dd>pass "p, div, a.someClass" to matches all paragraphs, divs and
     * anchors (with class "someClass")</dd>
     * </dl>
     * 
     * @param selector
     *            selector to use to match elements
     * @return new Doj instance
     */
    public abstract Doj get(String selector);

    /**
     * Shorthand for <code>get(selector).get(indexOfElement)</code>.
     * 
     * @param selector
     *            selector to use
     * @param index
     *            index of the element matching the selector to return
     * @return new Doj instance
     */
    public Doj get(final String selector, final int index) {
        return get(selector).get(index);
    }

    /**
     * Creates a new Doj instance containing all child elements of the current
     * context elements with the given tag.
     * 
     * @param tag
     *            tag to match
     * @return new Doj instance
     */
    public abstract Doj getByTag(String tag);

    /**
     * Shorthand for <code>attribute("id")</code>.
     * 
     * @return the id of the first context element
     * @see #attribute(java.lang.String)
     */
    public String id() {
        return attribute("id");
    }

    /**
     * Shorthand for <code>attribute("id")</code>.
     * 
     * @return the id of all context elements
     * @see #attributes(java.lang.String)
     */
    public String[] ids() {
        return attributes("id");
    }

    /**
     * Shorthand for <code>getByAttribute("class",
     * MatchType.CONTAINED_WITH_WHITESPACE, classToLookFor)</code>.
     * 
     * @param classToLookFor
     *            the class to match
     * @return new Doj instance
     */
    public Doj getByClass(final String classToLookFor) {
        return getByAttribute("class", MatchType.CONTAINED_WITH_WHITESPACE, classToLookFor);
    }

    /**
     * Shorthand for
     * <code>getByAttribute(attribute, MatchType.EQUALS, value)</code>.
     * 
     * @param attribute
     *            attribute to consider
     * @param value
     *            value to match exactly
     * @return new Doj instance
     */
    public Doj getByAttribute(final String attribute, final String value) {
        return getByAttribute(attribute, EQUALS, value);
    }

    /**
     * Creates a new Doj instance containing all child elements of the current
     * context elements with the given attribute matching the given value as
     * determined by the match type.
     * 
     * @param attribute
     *            attribute to consider
     * @param matchType
     *            type of match to make
     * @param value
     *            the value to match
     * @return new Doj instance
     */
    public abstract Doj getByAttribute(String attribute, MatchType matchType, String value);

    /**
     * Creates a new Doj instance containing all child elements of the current
     * context elements for which the value given attribute matches the given
     * pattern.
     * 
     * @param attribute
     *            attribute to use
     * @param pattern
     *            pattern the value of the attribute should match
     * @return new Doj instance
     * @see #getByAttributeMatching(java.lang.String, java.util.regex.Pattern)
     */
    public abstract Doj getByAttributeMatching(String attribute, String pattern);

    /**
     * Creates a new Doj instance containing all child elements of the current
     * context elements for which the value given attribute matches the given
     * pattern.
     * 
     * @param attribute
     *            attribute to use
     * @param pattern
     *            pattern the value of the attribute should match
     * @return new Doj instance
     * @see #getByAttributeMatching(java.lang.String, java.lang.String)
     */
    public abstract Doj getByAttributeMatching(String attribute, Pattern pattern);

    /**
     * Returns true if at least one of the context elements has the given class.
     * 
     * @param valueToContain
     *            class to check for
     * @return true if at least one of the context elements has the given class
     */
    public abstract boolean hasClass(String valueToContain);

    /**
     * Returns true if at least one of the context elements matches the tag.
     * 
     * @param tag
     *            tag to match
     * @return true if at least one of the context elements matches the tag
     */
    public abstract boolean is(String tag);

    /**
     * Checks the radiobuttons and checkboxes in the current context.
     * 
     * @return page resulting from checking the first input
     */
    public abstract Page check();

    /**
     * Unchecks the radiobuttons and checkboxes in the current context.
     * 
     * @return page resulting from unchecking the first input
     */
    public abstract Page uncheck();

    /**
     * Selects the options in the current context.
     * 
     * @return page resulting from selecting the first option
     */
    public abstract Page select();

    /**
     * Deselects the options in the current context.
     * 
     * @return page resulting from deselecting the first option
     */
    public abstract Page deselect();

    /**
     * Creates a new Doj instance by only retaining the elements that match the
     * given tag.
     * 
     * @param tag
     *            tag to match
     * @return new Doj instance
     */
    public abstract Doj withTag(String tag);

    /**
     * Creates a new Doj instance by only retaining the elements that contain
     * the given text, i.e.: <code>node.text().contains(textToContain)</code>
     * will return <code>true</code> for each node.
     * 
     * @param textToContain
     *            text the retained nodes should contain
     * @return new Doj instance
     */
    public abstract Doj withTextContaining(String textToContain);

    /**
     * Creates a new Doj instance by only retaining the elements that contain
     * text matching the given pattern.
     * 
     * @param pattern
     *            the pattern to match
     * @return new Doj instance
     * @see #withTextMatching(java.util.regex.Pattern)
     */
    public abstract Doj withTextMatching(String pattern);

    /**
     * Creates a new Doj instance by only retaining the elements that contain
     * text matching the given pattern.
     * 
     * @param pattern
     *            the pattern to match
     * @return new Doj instance
     * @see #withTextMatching(java.lang.String)
     */
    public abstract Doj withTextMatching(Pattern pattern);

    /**
     * Shorthand for
     * <code>withAttribute(key, MatchType.EXISTING, someValueOrEvenNull)</code>
     * 
     * @param key
     *            attribute to look for
     * @return a filtered context
     */
    public Doj with(final String key) {
        return withAttribute(key, MatchType.EXISTING, null);
    }

    /**
     * Shorthand for {@link #withAttribute(java.lang.String, java.lang.String)}.
     * 
     * @param key
     *            key to consider
     * @param value
     *            value to match exactly
     * @return new Doj instance
     */
    public Doj with(final String key, final String value) {
        return withAttribute(key, value);
    }

    /**
     * Shorthand for
     * <code>withAttribute(key, MatchType.EQUALS, someValueOrEvenNull)</code>
     * 
     * @param key
     *            key to consider
     * @param value
     *            value value to match exactly
     * @return new Doj instance
     */
    public Doj withAttribute(final String key, final String value) {
        return withAttribute(key, EQUALS, value);
    }

    /**
     * Creates a new Doj instance retaining only the context elements with the
     * given attribute matching the pattern.
     * 
     * @param key
     *            key of the attribute
     * @param pattern
     *            pattern the value of the attribute should match
     * @return new Doj instance
     * @see #withAttributeMatching(java.lang.String, java.util.regex.Pattern)
     */
    public abstract Doj withAttributeMatching(String key, String pattern);

    /**
     * Creates a new Doj instance retaining only the context elements with the
     * given attribute matching the pattern.
     * 
     * @param key
     *            key of the attribute
     * @param pattern
     *            pattern the value of the attribute should match
     * @return new Doj instance
     * @see #withAttributeMatching(java.lang.String, java.lang.String)
     */
    public abstract Doj withAttributeMatching(String key, Pattern pattern);

    /**
     * Creates a new Doj instance containing all context elements with the
     * attribute matching the given value, as determined by the match type.
     * 
     * @param key
     *            key to consider
     * @param matchType
     *            type of match
     * @param value
     *            value to match
     * @return new Doj instance
     */
    public abstract Doj withAttribute(String key, MatchType matchType, String value);

    /**
     * Shorthand for
     * <code>withAttribute(key, MatchType.CONTAINING, someValueOrEvenNull)</code>
     * 
     * @param key
     *            key to consider
     * @param value
     *            value that should be contained by the attribute
     * @return new Doj instance
     */
    public Doj withAttributeContaining(final String key, final String value) {
        return withAttribute(key, MatchType.CONTAINING, value);
    }

    /**
     * Shorthand for
     * <code>withAttribute("class", MatchType.CONTAINED_WITH_WHITESPACE, valueToContain)</code>
     * 
     * @param valueToContain
     *            value
     * @return new Doj instance
     */
    public Doj withClass(final String valueToContain) {
        return withAttribute("class", MatchType.CONTAINED_WITH_WHITESPACE, valueToContain);
    }

    /**
     * Shorthand for
     * <code>withAttribute("value", MatchType.EQUALS, valueToContain)</code>
     * 
     * @param valueToContain
     *            value
     * @return new Doj instance
     */
    public Doj withValue(final String valueToContain) {
        return withAttribute("value", MatchType.EQUALS, valueToContain);
    }

    /**
     * Shorthand for
     * <code>withAttribute("id", MatchType.EQUALS, valueToContain)</code>
     * 
     * @param valueToContain
     *            value
     * @return new Doj instance
     */
    public Doj withId(final String valueToContain) {
        return withAttribute("id", MatchType.EQUALS, valueToContain);
    }

    /**
     * Shorthand for
     * <code>withAttribute("type", MatchType.EQUALS, valueToContain)</code>
     * 
     * @param type
     *            type to match
     * @return new Doj instance
     */
    public Doj withType(final String type) {
        return withAttribute("type", type);
    }

    /**
     * Shorthand for
     * <code>withAttribute("name", MatchType.EQUALS, valueToContain)</code>
     * 
     * @param name
     *            name to match
     * @return new Doj instance
     */
    public Doj withName(final String name) {
        return withAttribute("name", name);
    }

    /**
     * Shorthand for
     * <code>hasAttribute(key, MatchType.EXISTING, anyValue)</code>
     * 
     * @param key
     *            of the attribute
     * @return new Doj instance
     */
    public boolean hasAttribute(final String key) {
        return hasAttribute(key, MatchType.EXISTING, null);
    }

    /**
     * Shorthand for <code>hasAttribute(key, MatchType.EQUALS, value)</code>
     * 
     * @param key
     *            of the attribute
     * @param value
     *            value to match
     * @return new Doj instance
     */
    public boolean hasAttribute(final String key, final String value) {
        return hasAttribute(key, MatchType.EQUALS, value);
    }

    /**
     * Shorthand for <code>hasAttribute("checked")</code>.
     * 
     * @return true when the first element is checked
     */
    public boolean isChecked() {
        return hasAttribute("checked");
    }

    /**
     * Shorthand for <code>hasAttribute("selected")</code>.
     * 
     * @return true when the first element is selected
     */
    public boolean isSelected() {
        return hasAttribute("selected");
    }

    /**
     * Shorthand for <code>hasAttribute("disabled")</code>.
     * 
     * @return true when the first element is disabled
     */
    public boolean isDisabled() {
        return hasAttribute("disabled");
    }

    /**
     * Shorthand for <code>hasAttribute("readonly")</code>.
     * 
     * @return true when the first element is readonly
     */
    public boolean isReadOnly() {
        return hasAttribute("readonly");
    }

    /**
     * Returns true if one of the context elements has a value for the attribute
     * matching the given value as defined by the match type.
     * 
     * @param key
     *            key of the attribute to match
     * @param matchType
     *            type of match to make
     * @param value
     *            the value to match
     * @return true if one of the context elements matches
     */
    public abstract boolean hasAttribute(String key, MatchType matchType, String value);

    /**
     * Returns the text content of the first context element.
     * 
     * @return the text content of the first context element
     */
    public abstract String text();

    /**
     * Returns the text contents of all context elements.
     * 
     * @return the text contents of all context elements
     */
    public abstract String[] texts();

    /**
     * Returns the trimmed text content of the first context element.
     * <p>
     * All whitespace characters, including newlines are condensed into a single
     * space - leading and trailing whitespace is removed.
     * </p>
     * 
     * @return the trimmed text content of the first context element
     */
    public abstract String trimmedText();

    /**
     * Returns the trimmed text contents of all context elements.
     * <p>
     * All whitespace characters, including newlines are condensed into a single
     * space - leading and trailing whitespace is removed.
     * </p>
     * 
     * @return the trimmed text contents of all context elements
     */
    public abstract String[] trimmedTexts();

    /**
     * Returns the value of the given attribute of the first context element.
     * 
     * @param key
     *            key of the attribute
     * @return the value of the given attribute of the first context element
     */
    public abstract String attribute(String key);

    /**
     * Returns the values of the given attribute of all context elements.
     * 
     * @param key
     *            key of the attribute
     * @return the values of the given attribute of all context elements
     */
    public abstract String[] attributes(String key);

    /**
     * Sets the attribute of each context element to the given value.
     * 
     * @param key
     *            key of the attribute
     * @param value
     *            value to set the attribute to
     * @return current instance
     */
    public abstract Doj attribute(String key, String value);

    /**
     * Shorthand for <code>attribute("name")</code>.
     * 
     * @return the value of the name attribute of the first element
     * @see #attribute(java.lang.String)
     */
    public String name() {
        return attribute("name");
    }

    /**
     * Shorthand for <code>attributes("name")</code>.
     * 
     * @return the value of the name attribute of the elements
     * @see #attributes(java.lang.String)
     */
    public String[] names() {
        return attributes("name");
    }

    /**
     * Shorthand for <code>attribute("type")</code>.
     * 
     * @return the value of the type attribute of the first element
     * @see #attribute(java.lang.String)
     */
    public String type() {
        return attribute("type");
    }

    /**
     * Shorthand for <code>attributes("type")</code>.
     * 
     * @return the value of the type attribute of the elements
     * @see #attributes(java.lang.String)
     */
    public String[] types() {
        return attributes("type");
    }

    /**
     * Shorthand for <code>attribute("class")</code>.
     * 
     * @return the value of the class attribute of the first element
     * @see #attribute(java.lang.String)
     */
    public String classValue() {
        return attribute("class");
    }

    /**
     * Shorthand for <code>attributes("class")</code>.
     * 
     * @return the value of the class attribute of the elements
     * @see #attributes(java.lang.String)
     */
    public String[] classValues() {
        return attributes("class");
    }

    /**
     * Returns the value of the first context element for input elements
     * (including textarea, select and button).
     * <p>
     * In the case of a select, the value of the first selected option is
     * returned.
     * </p>
     * <p>
     * <strong>Note:</strong> use {@link #values()} if you want all selected
     * options of a multiple select or if you want the values of all context
     * elements.
     * </p>
     * 
     * @return value of the first context element
     */
    public abstract String value();

    /**
     * Sets the value of the form input elements to the given value. In the case
     * of a multiple select, this will select an extra option.
     * 
     * @param value
     *            value to use
     * @return current Doj instance
     */
    public abstract Doj value(String value);

    /**
     * Returns the values of all form input context elements.
     * <p>
     * In the case of a select, the values of all selected options are returned.
     * </p>
     * 
     * @return the values of all form input context elements
     */
    public abstract String[] values();

    /**
     * Clicks on the first context element.
     * 
     * @return the result of clicking on the first context element
     * @throws java.io.IOException
     * @throws java.lang.ClassCastException
     */
    public abstract Page click() throws IOException, ClassCastException;

    /**
     * Returns the number of context elements.
     * 
     * @return the number of context elements
     */
    public abstract int size();

    /**
     * Returns true when there are no context elements.
     * 
     * @return true when there are no context elements
     */
    public abstract boolean isEmpty();

    /**
     * Creates a new Doj instance containing only the first context element
     * (wrapped).
     * 
     * @return new Doj instance
     */
    public abstract Doj first();

    /**
     * Returns the first context element (not wrapped).
     * 
     * @return the first context element (not wrapped)
     */
    public DomElement firstElement() {
        return getElement(0);
    }

    /**
     * Creates a new Doj instance containing only the last context element
     * (wrapped).
     * 
     * @return new Doj instance
     */
    public abstract Doj last();

    /**
     * Returns the last context element (not wrapped).
     * 
     * @return the last context element (not wrapped)
     */
    public DomElement lastElement() {
        return getElement(-1);
    }

    /**
     * Creates a new Doj instance containing the context elements resulting from
     * applying {@link #sliceElements(int, int)}.
     * 
     * @param startIndex
     *            index to start at - pass a negative number to work from the
     *            back to the front
     * @param nrItems
     *            number of items to slice
     * @return new Doj instance
     */
    public abstract Doj slice(int startIndex, int nrItems);

    /**
     * Returns the context elements from the start index.
     * <p>
     * When the start index falls out of bounds, it's limited to the closest
     * valid index (e.g. given 5 elements, an index of -6 is limited to 0 and an
     * index of 7 is limited to 4). The number of items is limited to the number
     * of items that can be retrieved (e.g. given 5 elements, with start index 4
     * and number of items 3, only one is returned).
     * </p>
     * 
     * @param startIndex
     *            index to start - pass a negative number to work from the back
     *            to the front
     * @param nrItems
     *            the number of items to slice
     * @return the context elements from the start index
     */
    public abstract DomElement[] sliceElements(int startIndex, int nrItems);

    /**
     * Returns all context elements.
     * 
     * @return all context elements
     */
    public DomElement[] allElements() {
        return sliceElements(0, size());
    }

    @Override
    public Iterator<Doj> iterator() {
        return new DojIterator(this);
    }

    /**
     * Throws an exception when the Doj instance is empty.
     * 
     * @return the current Doj instance
     * @throws DojHasNoNodesException
     */
    public abstract Doj verifyNotEmpty() throws DojIsEmptyException;

    /**
     * Factory method to create an initial Doj instance.
     * <p>
     * Hides the fact that there are two implementations of Doj at work behind
     * the scenes: one for working with an empty context that keeps the code for
     * the other one, with most of the logic, simple.
     * </p>
     * 
     * @param contextElements
     *            the context elements to use
     * @return new Doj instance
     */
    public static Doj on(final DomElement... contextElements) {
        return (contextElements == null || contextElements.length == 0 ? EMPTY : new NonEmptyDoj(contextElements)
        .unique());
    }

    /**
     * Factory method to create an initial Doj instance.
     * <p>
     * Hides the fact that there are two implementations of Doj at work behind
     * the scenes: one for working with an empty context that keeps the code for
     * the other one, with most of the logic, simple.
     * </p>
     * 
     * @param contextElements
     *            the context elements to use
     * @return new Doj instance
     */
    public static Doj on(final Collection<? extends DomElement> contextElements) {
        return (contextElements == null || contextElements.isEmpty() ? EMPTY : new NonEmptyDoj(contextElements)
        .unique());
    }

    /**
     * Factory method to create an initial Doj instance.
     * <p>
     * Hides the fact that there are two implementations of Doj at work behind
     * the scenes: one for working with an empty context that keeps the code for
     * the other one, with most of the logic, simple.
     * </p>
     * 
     * @param page
     *            the page supplying the document element
     * @return new Doj instance
     */
    public static Doj on(final HtmlPage page) {
        return (page == null ? EMPTY : new NonEmptyDoj(page));
    }

    /**
     * Iterator for looping over the context elements of a Doj instance.
     */
    private static class DojIterator implements Iterator<Doj> {

        private int index;
        private final Doj doj;

        public DojIterator(final Doj doj) {
            this.doj = doj;
        }

        @Override
        public boolean hasNext() {
            return index < doj.size();
        }

        @Override
        public Doj next() {
            return doj.get(index++);
        }

        @Override
        public void remove() {
            // Does nothing since a Doj object is immutable
        }
    }

    private static class NonEmptyDoj extends Doj {

        protected final DomElement[] contextElements;

        @Override
        public Doj get(final int index) {
            final DomElement element = getElement(index);
            return element == null ? EMPTY : on(element);
        }

        @Override
        public DomElement getElement(int index) {
            final int size = size();
            if (index < -size || index >= size) {
                return null;
            }
            index = (index >= 0 ? index : size + index);
            return contextElements[index];
        }

        @Override
        public Doj unique() {
            final Set<String> retained = new HashSet<String>();
            final List<DomElement> list = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                final String identifier = uniqueId(element);
                if (!retained.contains(identifier)) {
                    retained.add(identifier);
                    list.add(element);
                }
            }
            // This is a one-off: everything that creates a new Doj object
            // should pass via on(...) - but since unique is used all over,
            // this is the easiest way to make sure all Doj instances carry
            // unique context elements - without ending up in an infinite loop
            return list.isEmpty() ? EMPTY : new NonEmptyDoj(list);
        }

        protected String uniqueId(final DomElement element) {
            String identifier = element.getId();
            if (!StringUtils.isBlank(identifier)) {
                return identifier;
            }
            final String attribute = "data-doj-id";
            identifier = element.getAttribute(attribute);
            if (!org.apache.commons.lang3.StringUtils.isBlank(identifier)) {
                return identifier;
            }
            identifier = UUID.randomUUID().toString();
            element.setAttribute(attribute, identifier);
            return identifier;
        }

        @Override
        public Doj remove(final int index) {
            final int size = size();
            if (index < -size || index >= size) {
                return this;
            }
            if (size == 1 && (index == 0 || index == -1)) {
                return EMPTY;
            }
            final int indexElementToRemove = (index >= 0 ? index : size + index);
            final DomElement[] newContextElements = new DomElement[size - 1];
            for (int loop = 0; loop < size; ++loop) {
                if (loop == indexElementToRemove) {
                    continue;
                }
                newContextElements[loop < indexElementToRemove ? loop : loop - 1] = contextElements[loop];
            }
            return on(newContextElements);
        }

        @Override
        public Doj get(final String selectorString) {
            final List<List<DojCssSelector>> selectorList = new DojCssSelector().compile(selectorString);
            Doj all = EMPTY;
            for (final List<DojCssSelector> selectors : selectorList) {
                Doj doj = this;
                boolean descend = true;
                for (final DojCssSelector selector : selectors) {
                    if (selector.getType() == DojCssSelector.Type.DESCENDANT) {
                        descend = true;
                    } else {
                        doj = applySimpleSelector(selector, doj, descend);
                        descend = false;
                    }
                }
                all = all.merge(doj);
            }
            return all;
        }

        protected Doj applySimpleSelector(final DojCssSelector selector, final Doj doj, final boolean descend) {
            final String value = selector.getValue();
            switch (selector.getType()) {
            case ELEMENT:
                return descend ? doj.getByTag(value) : doj.withTag(value);
            case HTML_CLASS:
                return descend ? doj.getByClass(value) : doj.withClass(value);
            case ID:
                return descend ? doj.getById(value) : doj.withId(value);
            }
            return EMPTY;
        }

        @Override
        public Doj getById(final String id) {
            for (final DomElement element : contextElements) {
                try {

                    final DomElement elementWithId = (DomElement) element.querySelector("#" + id);
                    if (elementWithId != null) {
                        return on(elementWithId);
                    }
                } catch (final ElementNotFoundException e) {
                    // Ignore
                }
            }
            return EMPTY;
        }

        @Override
        public Doj getByTag(final String tag) {
            final List<DomElement> list = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                list.addAll(element.getElementsByTagName(tag));
            }
            return on(list);
        }

        @Override
        public Doj getByAttribute(final String attribute, final MatchType matchType, final String value) {
            final List<DomElement> list = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                for (final DomElement child : element.getChildElements()) {
                    if (matchType.isMatch(child.getAttribute(attribute), value)) {
                        list.add(child);
                    }
                }
            }
            return on(list);
        }

        @Override
        public boolean hasClass(final String valueToContain) {
            return hasAttribute("class", MatchType.CONTAINED_WITH_WHITESPACE, valueToContain);
        }

        @Override
        public boolean is(final String tag) {
            for (final DomElement element : contextElements) {
                if (tag.equalsIgnoreCase(element.getTagName())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Doj withTag(final String tag) {
            final List<DomElement> list = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                if (tag.equalsIgnoreCase(element.getTagName())) {
                    list.add(element);
                }
            }
            return on(list);
        }

        @Override
        public Doj withAttribute(final String key, final MatchType matchType, final String value) {
            final List<DomElement> list = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                if (matchType.isMatch(element.getAttribute(key), value)) {
                    list.add(element);
                }
            }
            return on(list);
        }

        @Override
        public boolean hasAttribute(final String key, final MatchType matchType, final String value) {
            for (final DomElement element : contextElements) {
                if (matchType.isMatch(element.getAttribute(key), value)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String text() {
            return firstElement().getTextContent();
        }

        @Override
        public String[] texts() {
            final int size = size();
            final String[] texts = new String[size];
            for (int index = 0; index < size; ++index) {
                texts[index] = contextElements[index].getTextContent();
            }
            return texts;
        }

        @Override
        public String trimmedText() {
            final String text = text();
            return text == null ? null : text.replaceAll("\\s+", " ").trim();
        }

        @Override
        public String[] trimmedTexts() {
            final int size = size();
            final String[] texts = new String[size];
            for (int index = 0; index < size; ++index) {
                final String text = contextElements[index].getTextContent();
                texts[index] = (text == null ? null : text.replaceAll("\\s+", " ").trim());
            }
            return texts;
        }

        @Override
        public String attribute(final String key) {
            return firstElement().getAttribute(key);
        }

        @Override
        public String[] attributes(final String key) {
            final int length = contextElements.length;
            final String[] values = new String[length];
            for (int index = 0; index < length; ++index) {
                values[index] = contextElements[index].getAttribute(key);
            }
            return values;
        }

        @Override
        public Doj attribute(final String key, final String value) {
            for (final DomElement element : contextElements) {
                element.setAttribute(key, value);
            }
            return this;
        }

        @Override
        public String value() {
            final DomElement first = firstElement();
            if ("textarea".equalsIgnoreCase(first.getTagName())) {
                return ((HtmlTextArea) first).getText();
            }
            if ("select".equalsIgnoreCase(first.getTagName())) {
                return first().get("option").with("selected", "selected").value();
            }
            if ("option".equalsIgnoreCase(first.getTagName())) {
                return ((HtmlOption) first).getValueAttribute();
            }
            if ("input".equalsIgnoreCase(first.getTagName())) {
                return ((HtmlInput) first).getValueAttribute();
            }
            if ("button".equalsIgnoreCase(first.getTagName())) {
                return ((HtmlButton) first).getValueAttribute();
            }
            return null;
        }

        @Override
        public String[] values() {
            final List<String> values = new ArrayList<String>();
            for (final Doj element : this) {
                if (element.is("select") && element.hasAttribute("multiple", "multiple")) {
                    final Doj selectedOptions = element.get("option").with("selected", "selected");
                    for (final Doj option : selectedOptions) {
                        values.add(option.value());
                    }
                } else {
                    values.add(element.value());
                }
            }
            return values.toArray(new String[values.size()]);
        }

        @Override
        public Page click() throws IOException, ClassCastException {
            return firstElement().click();
        }

        @Override
        public int size() {
            return contextElements.length;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Doj first() {
            return on(firstElement());
        }

        @Override
        public Doj last() {
            return on(lastElement());
        }

        @Override
        public Doj slice(final int startIndex, final int nrItems) {
            return on(sliceElements(startIndex, nrItems));
        }

        @Override
        public DomElement[] sliceElements(int startIndex, int nrItems) {
            final int size = size();
            if (startIndex < 0) {
                nrItems = (nrItems > -startIndex ? -startIndex : nrItems);
                startIndex = size + startIndex;
                startIndex = (startIndex < 0 ? 0 : startIndex);
            }
            if (startIndex > size) {
                startIndex = size - 1;
            }
            nrItems = Math.min(size - startIndex, nrItems);
            final DomElement[] result = new DomElement[nrItems];
            for (int index = startIndex; index < nrItems + startIndex; ++index) {
                result[index - startIndex] = contextElements[index];
            }
            return result;
        }

        public NonEmptyDoj(final DomElement... contextElements) {
            this.contextElements = contextElements;
        }

        public NonEmptyDoj(final Collection<? extends DomElement> contextElements) {
            this.contextElements = new DomElement[contextElements.size()];
            int index = -1;
            for (final DomElement element : contextElements) {
                this.contextElements[++index] = element;
            }
        }

        public NonEmptyDoj(final HtmlPage page) {
            this.contextElements = new DomElement[] { page.getDocumentElement() };
        }

        @Override
        public Doj next() {
            final List<DomElement> siblings = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                DomNode node = element.getNextSibling();
                while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
                    node = node.getNextSibling();
                }
                if (node != null) {
                    siblings.add((DomElement) node);
                }
            }
            return on(siblings);
        }

        @Override
        public Doj next(final String tag) {
            final List<DomElement> siblings = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                DomNode node = element.getNextSibling();
                while (node != null
                        && (node.getNodeType() != Node.ELEMENT_NODE || !node.getNodeName().equalsIgnoreCase(tag))) {
                    node = node.getNextSibling();
                }
                if (node != null && node.getNodeName().equalsIgnoreCase(tag)) {
                    siblings.add((DomElement) node);
                }
            }
            return on(siblings);
        }

        @Override
        public Doj previous() {
            final List<DomElement> siblings = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                DomNode node = element.getPreviousSibling();
                while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
                    node = node.getPreviousSibling();
                }
                if (node != null) {
                    siblings.add((DomElement) node);
                }
            }
            return on(siblings);
        }

        @Override
        public Doj previous(final String tag) {
            final List<DomElement> siblings = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                DomNode node = element.getPreviousSibling();
                while (node != null
                        && (node.getNodeType() != Node.ELEMENT_NODE || !node.getNodeName().equalsIgnoreCase(tag))) {
                    node = node.getPreviousSibling();
                }
                if (node != null && node.getNodeName().equalsIgnoreCase(tag)) {
                    siblings.add((DomElement) node);
                }
            }
            return on(siblings);
        }

        @Override
        public Doj parent() {
            final List<DomElement> parents = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                final DomElement parent = (DomElement) element.getParentNode();
                if (parent != null) {
                    parents.add(parent);
                }
            }
            return on(parents);
        }

        @Override
        public Doj parent(final String tag) {
            final List<DomElement> parents = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                DomElement parent = (DomElement) element.getParentNode();
                while (parent != null && !parent.getTagName().equalsIgnoreCase(tag)) {
                    parent = (DomElement) parent.getParentNode();
                }
                if (parent != null) {
                    parents.add(parent);
                }
            }
            return on(parents);
        }

        @Override
        public Doj verifyNotEmpty() throws DojIsEmptyException {
            return this;
        }

        @Override
        public Doj value(final String value) {
            for (final DomElement element : contextElements) {
                if ("textarea".equalsIgnoreCase(element.getTagName())) {
                    ((HtmlTextArea) element).setText(value);
                } else if ("select".equalsIgnoreCase(element.getTagName())) {
                    ((HtmlSelect) element).setSelectedAttribute(value, true);
                } else if ("button".equalsIgnoreCase(element.getTagName())) {
                    ((HtmlButton) element).setValueAttribute(value);
                } else {
                    ((HtmlInput) element).setValueAttribute(value);
                }
            }
            return this;
        }

        @Override
        public Doj withTextContaining(final String textToContain) {
            final List<DomElement> retained = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                final String text = element.asText();
                if (text != null && text.contains(textToContain)) {
                    retained.add(element);
                }
            }
            return on(retained);
        }

        @Override
        public Doj withTextMatching(final String pattern) {
            return withTextMatching(Pattern.compile(pattern));
        }

        @Override
        public Doj withTextMatching(final Pattern pattern) {
            final List<DomElement> retained = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                final String text = element.asText();
                if (text != null && pattern.matcher(text).matches()) {
                    retained.add(element);
                }
            }
            return on(retained);
        }

        @Override
        public Doj withAttributeMatching(final String key, final String pattern) {
            return withAttributeMatching(key, Pattern.compile(pattern));
        }

        @Override
        public Doj withAttributeMatching(final String key, final Pattern pattern) {
            final List<DomElement> list = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                if (pattern.matcher(element.getAttribute(key)).matches()) {
                    list.add(element);
                }
            }
            return on(list);
        }

        @Override
        public Doj getByAttributeMatching(final String attribute, final String pattern) {
            return getByAttributeMatching(attribute, Pattern.compile(pattern));
        }

        @Override
        public Doj getByAttributeMatching(final String attribute, final Pattern pattern) {
            final List<DomElement> list = new ArrayList<DomElement>();
            for (final DomElement element : contextElements) {
                for (final DomElement child : element.getChildElements()) {
                    if (pattern.matcher(child.getAttribute(attribute)).matches()) {
                        list.add(child);
                    }
                }
            }
            return on(list);
        }

        @Override
        public Page check() {
            return check(true);
        }

        @Override
        public Page uncheck() {
            return check(false);
        }

        protected Page check(final boolean toCheck) {
            Page page = null;
            Doj matches = this.withType("radio");
            for (final DomElement radiobutton : matches.allElements()) {
                final Page temp = ((HtmlRadioButtonInput) radiobutton).setChecked(toCheck);
                if (page == null) {
                    page = temp;
                }
            }
            matches = this.withType("checkbox");
            for (final DomElement checkbox : matches.allElements()) {
                final Page temp = ((HtmlCheckBoxInput) checkbox).setChecked(toCheck);
                if (page == null) {
                    page = temp;
                }
            }
            return page;
        }

        @Override
        public Page select() {
            return select(true);
        }

        @Override
        public Page deselect() {
            return select(false);
        }

        protected Page select(final boolean toSelect) {
            Page page = null;
            final Doj matches = this.withTag("option");
            for (final DomElement option : matches.allElements()) {
                final Page temp = ((HtmlOption) option).setSelected(toSelect);
                if (page == null) {
                    page = temp;
                }
            }
            return page;
        }
    }

    /**
     * Implementation of an empty Doj object - helps keep the other code simple.
     */
    private static class EmptyDoj extends Doj {

        private static final DomElement[] EMPTY_ELEMENT_ARRAY = new DomElement[0];
        private static final String[] EMPTY_STRING_ARRAY = new String[0];

        @Override
        public Doj unique() {
            return this;
        }

        @Override
        public Doj withTag(final String tag) {
            return this;
        }

        @Override
        public String attribute(final String key) {
            return null;
        }

        @Override
        public Doj attribute(final String key, final String value) {
            return this;
        }

        @Override
        public String[] attributes(final String key) {
            return EMPTY_STRING_ARRAY;
        }

        @Override
        public Page click() throws IOException {
            return null;
        }

        @Override
        public Doj first() {
            return this;
        }

        @Override
        public Doj getByAttribute(final String attribute, final MatchType matchType, final String value) {
            return this;
        }

        @Override
        public Doj getById(final String id) {
            return this;
        }

        @Override
        public Doj getByTag(final String tag) {
            return this;
        }

        @Override
        public DomElement getElement(final int index) {
            return null;
        }

        @Override
        public boolean hasAttribute(final String key, final MatchType matchType, final String value) {
            return false;
        }

        @Override
        public boolean hasClass(final String valueToContain) {
            return false;
        }

        @Override
        public boolean is(final String tag) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Doj last() {
            return this;
        }

        @Override
        public Doj next() {
            return this;
        }

        @Override
        public Doj next(final String tag) {
            return this;
        }

        @Override
        public Doj parent() {
            return this;
        }

        @Override
        public Doj parent(final String tag) {
            return this;
        }

        @Override
        public Doj previous() {
            return this;
        }

        @Override
        public Doj previous(final String tag) {
            return this;
        }

        @Override
        public Doj remove(final int index) {
            return this;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Doj slice(final int startIndex, final int nrItems) {
            return this;
        }

        @Override
        public DomElement[] sliceElements(final int startIndex, final int nrItems) {
            return EMPTY_ELEMENT_ARRAY;
        }

        @Override
        public String text() {
            return null;
        }

        @Override
        public String[] texts() {
            return EMPTY_STRING_ARRAY;
        }

        @Override
        public String trimmedText() {
            return null;
        }

        @Override
        public String[] trimmedTexts() {
            return EMPTY_STRING_ARRAY;
        }

        @Override
        public String value() {
            return null;
        }

        @Override
        public String[] values() {
            return EMPTY_STRING_ARRAY;
        }

        @Override
        public Doj withAttribute(final String key, final MatchType matchType, final String value) {
            return this;
        }

        @Override
        public Doj get(final int index) {
            return this;
        }

        @Override
        public Doj get(final String selector) {
            return this;
        }

        @Override
        public Doj verifyNotEmpty() throws DojIsEmptyException {
            throw new DojIsEmptyException();
        }

        @Override
        public Doj value(final String value) {
            return this;
        }

        @Override
        public Doj withTextContaining(final String textToContain) {
            return this;
        }

        @Override
        public Doj withTextMatching(final String pattern) {
            return this;
        }

        @Override
        public Doj withTextMatching(final Pattern pattern) {
            return this;
        }

        @Override
        public Doj withAttributeMatching(final String key, final String pattern) {
            return this;
        }

        @Override
        public Doj withAttributeMatching(final String key, final Pattern pattern) {
            return this;
        }

        @Override
        public Doj getByAttributeMatching(final String attribute, final String pattern) {
            return this;
        }

        @Override
        public Doj getByAttributeMatching(final String attribute, final Pattern pattern) {
            return this;
        }

        @Override
        public Page check() {
            return null;
        }

        @Override
        public Page uncheck() {
            return null;
        }

        @Override
        public Page select() {
            return null;
        }

        @Override
        public Page deselect() {
            return null;
        }
    }

}
