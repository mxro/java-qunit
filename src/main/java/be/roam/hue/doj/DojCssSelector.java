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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Selector object and compiler for simple CSS selectors.
 * @author Kevin Wetzels
 */
public class DojCssSelector {

    public enum Type {
        ELEMENT,
        HTML_CLASS,
        ID,
        DESCENDANT
    }

    private static final DojCssSelector DESCENDANT_SELECTOR = new DojCssSelector(Type.DESCENDANT, " ");

    private Type type;

    private String value;

    public DojCssSelector() {
        super();
    }

    public DojCssSelector(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<List<DojCssSelector>> compile(String groupSelector) {
        List<List<DojCssSelector>> result = new ArrayList<List<DojCssSelector>>();
        String[] parts = groupSelector.split(",");
        for (String part : parts) {
            part = part.trim();
            if (StringUtils.isBlank(part)) {
                continue;
            }
            List<DojCssSelector> compiled = compileSingle(part);
            if (!compiled.isEmpty()) {
                result.add(compiled);
            }
        }
        return result;
    }
    
    public List<DojCssSelector> compileSingle(String selector) {
        List<DojCssSelector> result = new ArrayList<DojCssSelector>();
        String[] parts = selector.split("\\s");
        boolean first = true;
        for (String part : parts) {
            part = part.trim();
            if (StringUtils.isBlank(part)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                result.add(DESCENDANT_SELECTOR);
            }
            compileSimpleSelector(part, result);            
        }
        return result;
    }

    protected void compileSimpleSelector(String selector, List<DojCssSelector> list) {
        List<String> parts = tokenize(selector);
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            int firstCharacter = part.charAt(0);
            if (firstCharacter == '.') {
                list.add(new DojCssSelector(Type.HTML_CLASS, part.substring(1)));
            } else if (firstCharacter == '#') {
                list.add(new DojCssSelector(Type.ID, part.substring(1)));
            } else {
                list.add(new DojCssSelector(Type.ELEMENT, part));
            }
        }
    }

    protected List<String> tokenize(String selector) {
        List<String> tokens = new ArrayList<String>();
        int previous = 0;
        for (int index = 0, max = selector.length(); index < max; ++index) {
            char character = selector.charAt(index);
            if (index > 0 && (character == '.' || character == '#')) {
                tokens.add(selector.substring(previous, index));
                previous = index;
            }
        }
        tokens.add(selector.substring(previous));
        return tokens;
    }

}
