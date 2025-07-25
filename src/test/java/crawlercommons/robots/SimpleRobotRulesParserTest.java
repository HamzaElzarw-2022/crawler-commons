/**
 * Copyright 2016 Crawler-Commons
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package crawlercommons.robots;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleRobotRulesParserTest {
    private static final String LF = "\n";
    private static final String CR = "\r";
    private static final String CRLF = "\r\n";
    private static final String FAKE_ROBOTS_URL = "http://domain.com";

    private static BaseRobotRules createRobotRules(String crawlerName, String content) {
        return createRobotRules(crawlerName, content.getBytes(UTF_8), true);
    }

    public static BaseRobotRules createRobotRules(String crawlerName, String content, boolean exactUserAgentMatching) {
        return createRobotRules(crawlerName, content.getBytes(UTF_8), exactUserAgentMatching);
    }

    public static BaseRobotRules createRobotRules(String crawlerName, byte[] contentBytes) {
        return createRobotRules(crawlerName, contentBytes, true);
    }

    private static BaseRobotRules createRobotRules(String crawlerName, byte[] contentBytes, boolean exactUserAgentMatching) {
        SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
        robotParser.setExactUserAgentMatching(exactUserAgentMatching);
        return robotParser.parseContent(FAKE_ROBOTS_URL, contentBytes, "text/plain", crawlerName);
    }

    public static BaseRobotRules createRobotRules(String[] crawlerNames, String content, boolean exactUserAgentMatching) {
        return createRobotRules(crawlerNames, content.getBytes(UTF_8), exactUserAgentMatching);
    }

    public static BaseRobotRules createRobotRules(String[] crawlerNames, byte[] contentBytes, boolean exactUserAgentMatching) {
        return createRobotRules(Arrays.asList(crawlerNames), contentBytes, exactUserAgentMatching);
    }

    public static BaseRobotRules createRobotRules(Collection<String> crawlerNames, String content, boolean exactUserAgentMatching) {
        return createRobotRules(crawlerNames, content.getBytes(UTF_8), exactUserAgentMatching);
    }

    public static BaseRobotRules createRobotRules(Collection<String> crawlerNames, byte[] contentBytes, boolean exactUserAgentMatching) {
        SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
        robotParser.setExactUserAgentMatching(exactUserAgentMatching);
        return robotParser.parseContent(FAKE_ROBOTS_URL, contentBytes, "text/plain", crawlerNames);
    }

    @Test
    void testEmptyRules() {
        BaseRobotRules rules = createRobotRules("anybot", "");
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testQueryParamInDisallow() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /index.cfm?fuseaction=sitesearch.results*";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertFalse(rules.isAllowed("http://searchservice.domain.com/index.cfm?fuseaction=sitesearch.results&type=People&qry=california&pg=2"));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.com/fish", //
                    "False, http://www.fict.com/fish.html", //
                    "False, http://www.fict.com/fish/salmon.html", //
                    "False, http://www.fict.com/fishheads", //
                    "False, http://www.fict.com/fishheads/yummy.html", //
                    "False, http://www.fict.com/fish.php?id=anything", //
                    "True, http://www.fict.com/Fish.asp", //
                    "True, http://www.fict.com/catfish", //
                    "True, http://www.fict.com/?id=fish", //
                    "True, http://www.fict.com/fis" })
    void testGooglePatternMatching1(boolean isAllowed, String urlStr) {
        // Test for /fish
        final String simpleRobotsTxt1 = "User-agent: *" + CRLF //
                        + "Disallow: /fish" + CRLF;
        BaseRobotRules rule1 = createRobotRules("anybot", simpleRobotsTxt1);
        assertEquals(isAllowed, rule1.isAllowed(urlStr));

        // Test for /fish*
        final String simpleRobotsTxt2 = "User-agent: *" + CRLF //
                        + "Disallow: /fish*" + CRLF;
        BaseRobotRules rule2 = createRobotRules("anybot", simpleRobotsTxt2);
        assertEquals(isAllowed, rule2.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.com/fish/", //
                    "False, http://www.fict.com/fish/?id=anything", //
                    "False, http://www.fict.com/fish/salmon.htm", //
                    "True, http://www.fict.com/fish", //
                    "True, http://www.fict.com/fish.html", //
                    "True, http://www.fict.com/Fish/Salmon.asp" })
    void testGooglePatternMatching2(boolean isAllowed, String urlStr) {
        // Test for /fish
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /fish/" + CRLF;
        BaseRobotRules rule = createRobotRules("anybot", simpleRobotsTxt);
        assertEquals(isAllowed, rule.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.com/filename.php", //
                    "False, http://www.fict.com/folder/filename.php", //
                    "False, http://www.fict.com/folder/filename.php?parameters", //
                    "False, http://www.fict.com/folder/any.php.file.html", //
                    "False, http://www.fict.com/filename.php/", //
                    "True, http://www.fict.com/", //
                    "True, http://www.fict.com/windows.PHP" })
    void testGooglePatternMatching3(boolean isAllowed, String urlStr) {
        // Test for /*.php
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /*.php" + CRLF;
        BaseRobotRules rule = createRobotRules("anybot", simpleRobotsTxt);
        assertEquals(isAllowed, rule.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.com/filename.php", //
                    "False, http://www.fict.com/folder/filename.php", //
                    "True, http://www.fict.com/filename.php?parameters", //
                    "True, http://www.fict.com/filename.php/", //
                    "True, http://www.fict.com/filename.php5", //
                    "True, http://www.fict.com/windows.PHP" })
    void testGooglePatternMatching4(boolean isAllowed, String urlStr) {
        // Test for /*.php$
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /*.php$" + CRLF;
        BaseRobotRules rule = createRobotRules("anybot", simpleRobotsTxt);
        assertEquals(isAllowed, rule.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.com/fish.php", //
                    "False, http://www.fict.com/fishheads/catfish.php?parameters", //
                    "True, http://www.fict.com/Fish.PHP" })
    void testGooglePatternMatching5(boolean isAllowed, String urlStr) {
        // Test for /fish*.php
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /fish*.php" + CRLF;
        BaseRobotRules rule = createRobotRules("anybot", simpleRobotsTxt);
        assertEquals(isAllowed, rule.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.com/fish.php", //
                    "False, http://www.fict.com/superfishheads/catfish.php?parameters", //
                    "True, http://www.fict.com/fishheads/catfish.htm" })
    void testGooglePatternMatching6(boolean isAllowed, String urlStr) {
        // Test rule with multiple '*' characters
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /*fish*.php" + CRLF;
        BaseRobotRules rule = createRobotRules("anybot", simpleRobotsTxt);
        assertEquals(isAllowed, rule.isAllowed(urlStr));
    }

    @Test
    void testCommentedOutLines() {
        final String simpleRobotsTxt = "#user-agent: testAgent" + LF + LF //
                        + "#allow: /index.html" + LF //
                        + "#allow: /test" + LF //
                        + LF //
                        + "#user-agent: test" + LF + LF //
                        + "#allow: /index.html" + LF //
                        + "#disallow: /test" + LF //
                        + LF //
                        + "#user-agent: someAgent" + LF + LF //
                        + "#disallow: /index.html" + LF //
                        + "#disallow: /test" + LF + LF;

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testRobotsTxtAlwaysAllowed() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /";

        BaseRobotRules rules = createRobotRules("any-darn-crawler", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/robots.txt"));
    }

    @Test
    void testAgentNotListed() {
        // Access is assumed to be allowed, if no rules match an agent.
        final String simpleRobotsTxt = "User-agent: crawler1" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /" + CRLF //
                        + CRLF //
                        + "User-agent: crawler2" + CRLF //
                        + "Disallow: /";

        BaseRobotRules rules = createRobotRules("crawler3", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/index.html"));
    }

    @Test
    void testNonAsciiEncoding() {
        final String simpleRobotsTxt = "User-agent: *" + " # \u00A2 \u20B5" + CRLF //
                        + "Disallow:";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testUnicodeUnescapedPaths() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /bücher/" + CRLF //
                        + "Disallow: /k%C3%B6nyvek/" + CRLF //
                        + CRLF //
                        + "User-agent: GoodBot" + CRLF //
                        + "Allow: /";

        BaseRobotRules rules = createRobotRules("mybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("https://www.example.com/"));

        // test using escaped and unescaped URLs
        assertFalse(rules.isAllowed("https://www.example.com/b%C3%BCcher/book1.html"));
        assertFalse(rules.isAllowed("https://www.example.com/bücher/book2.html"));

        // (for completeness) check also escaped path in robots.txt
        assertFalse(rules.isAllowed("https://www.example.com/k%C3%B6nyvek/book1.html"));
        assertFalse(rules.isAllowed("https://www.example.com/könyvek/book2.html"));

        // test invalid encoding: invalid encoded characters should not break
        // parsing of rules below
        rules = createRobotRules("goodbot", simpleRobotsTxt.getBytes(StandardCharsets.ISO_8859_1));
        assertTrue(rules.isAllowed("https://www.example.com/"));
        assertTrue(rules.isAllowed("https://www.example.com/b%C3%BCcher/book1.html"));

        // test invalid encoding: only rules with invalid characters should be
        // ignored
        rules = createRobotRules("mybot", simpleRobotsTxt.getBytes(StandardCharsets.ISO_8859_1));
        assertTrue(rules.isAllowed("https://www.example.com/"));
        assertFalse(rules.isAllowed("https://www.example.com/k%C3%B6nyvek/book1.html"));
        assertFalse(rules.isAllowed("https://www.example.com/könyvek/book2.html"));
        // if URL paths in disallow rules are not properly encoded, these two
        // URLs are not matched:
        // assertFalse(rules.isAllowed("https://www.example.com/b%C3%BCcher/book2.html"));
        // assertFalse(rules.isAllowed("https://www.example.com/bücher/book1.html"));
    }

    @ParameterizedTest
    @CsvSource({ // Tests for percent-encoded characters with special semantics
                 // in allow/disallow statements:
                 // (a) must not trim percent-encoded white space
                    "True, /*%20, https://www.example.com/", //
                    "False, /*%20, https://www.example.com/foobar%20/", //
                    "True, /*%20, https://www.example.com/foobar/", //
                    // (b) match literal %2F in URL path, but do not match a
                    // slash
                    "True, /*%2F*, https://www.example.com/path/index.html", //
                    "False, /*%2F*, https://www.example.com/topic/9%2F11/index.html", //
                    "False, /topic/9%2F11/, https://www.example.com/topic/9%2F11/index.html", //
                    "False, /topic/9%2F11/, https://www.example.com/topic/9%2f11/index.html", //
                    "False, /q?*mime=application%2Fpdf, https://www.example.com/q?mime=application%2Fpdf", //
                    // (c) percent-encoded dollar and asterisk (*)
                    "False, /$, https://www.example.com/", //
                    "True, /$, https://www.example.com/foobar", //
                    "True, /%24, https://www.example.com/", //
                    "False, /%24, https://www.example.com/%24100", //
                    "False, /%24, https://www.example.com/$100", //
                    "True, /search/%2A/, https://www.example.com/search/foobar/", //
                    "False, /search/%2A/, https://www.example.com/search/%2A/", //
                    "False, /search/%2A/, https://www.example.com/search/%2a/", //
                    "False, /search/%2a/, https://www.example.com/search/%2a/", //
                    "False, /search/%2a/, https://www.example.com/search/*/", //
                    "False, /search/*/, https://www.example.com/search/foobar/", //
                    // examples from RFC 9309, 2.2.2. The "Allow" and "Disallow"
                    // Lines
                    // https://www.rfc-editor.org/rfc/rfc9309.html#name-the-allow-and-disallow-line
                    "False, /foo/bar?baz=quz, https://www.example.com/foo/bar?baz=quz", //
                    // See the comment in
                    // https://github.com/google/robotstxt/blob/master/robots_test.cc
                    // "Percent encoding URIs in the rules is unnecessary."
                    // and "/foo/bar?baz=http://foo.bar stays unencoded."
                    "False, /foo/bar?baz=https://foo.bar, https://www.example.com/foo/bar?baz=https://foo.bar", //
                    "False, /foo/bar?baz=https%3A%2F%2Ffoo.bar, https://www.example.com/foo/bar?baz=https%3A%2F%2Ffoo.bar", //
                    "False, /foo/bar/\u30C4, https://www.example.com/foo/bar/%E3%83%84", //
                    "False, /foo/bar/%E3%83%84, https://www.example.com/foo/bar/%E3%83%84", //
                    "False, /foo/bar/%62%61%7A, https://www.example.com/foo/bar/baz", //
                    // examples from RFC 9309, 2.2.3. Special Characters
                    // https://www.rfc-editor.org/rfc/rfc9309.html#name-special-characters
                    "False, /path/file-with-a-%2A.html, https://www.example.com/path/file-with-a-*.html", //
                    "True, /path/file-with-a-%2A.html, https://www.example.com/path/file-with-a-foo.html", //
                    "False, /path/file-with-a-%2A.html, https://www.example.com/path/file-with-a-%2A.html", //
                    "False, /path/foo-%24, https://www.example.com/path/foo-$", //
                    "True, /path/foo-%24, https://www.example.com/path/foo-", //
                    "False, /path/foo-%24, https://www.example.com/path/foo-%24", //
    })
    void testEscapedPaths(boolean isAllowed, String disallowPath, String urlStr) {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: " + disallowPath + CRLF //
                        + "Allow: /";
        BaseRobotRules rules = createRobotRules("mybot", simpleRobotsTxt);
        String msg = urlStr + " should " + (isAllowed ? "not" : "") + " be disallowed by rule Disallow: " + disallowPath;
        assertEquals(isAllowed, rules.isAllowed(urlStr), msg);
    }

    @Test
    void testSimplestAllowAll() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow:";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    // https://github.com/crawler-commons/crawler-commons/issues/215
    @Test
    void testDisallowWithQueryOnly() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertFalse(rules.isAllowed("http://www.example.com"));
        assertFalse(rules.isAllowed("http://www.example.com?q=a"));
    }

    @ParameterizedTest
    @CsvSource({ "True, http://www.fict.org/", //
                    "True, http://www.fict.org/index.html" })
    void testMixedEndings1(boolean isAllowed, String urlStr) {
        final String mixedEndingsRobotsTxt = "# /robots.txt for http://www.fict.org/" + CRLF //
                        + "# comments to webmaster@fict.org" + CRLF //
                        + "User-agent: unhipbot" + LF //
                        + "Disallow: /" + CR //
                        + CRLF //
                        + "User-agent: webcrawler" + LF //
                        + "User-agent: excite" + CR //
                        + "Disallow: " + "\u0085" + CR //
                        + "User-agent: *" + CRLF //
                        + "Disallow: /org/plans.html" + LF //
                        + "Allow: /org/" + CR //
                        + "Allow: /serv" + CRLF //
                        + "Allow: /~mak" + LF //
                        + "Disallow: /" + CRLF;

        BaseRobotRules rules = createRobotRules("WebCrawler/3.0", mixedEndingsRobotsTxt, false);
        assertEquals(isAllowed, rules.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.org/", //
                    "False, http://www.fict.org/index.html", //
                    "True, http://www.fict.org/robots.txt", //
                    "True, http://www.fict.org/server.html", //
                    "True, http://www.fict.org/services/fast.html", //
                    "True, http://www.fict.org/services/slow.html", //
                    "False, http://www.fict.org/orgo.gif", //
                    "True, http://www.fict.org/org/about.html", //
                    "False, http://www.fict.org/org/plans.html", //
                    "False, http://www.fict.org/%7Ejim/jim.html", //
                    "True, http://www.fict.org/%7Emak/mak.html" })
    void testMixedEndings2(boolean isAllowed, String urlStr) {
        final String mixedEndingsRobotsTxt = "# /robots.txt for http://www.fict.org/" + CRLF //
                        + "# comments to webmaster@fict.org" + LF + CR //
                        + "User-agent: unhipbot" + LF //
                        + "Disallow: /" + CR //
                        + "" + CRLF //
                        + "User-agent: webcrawler" + LF //
                        + "User-agent: excite" + CR //
                        + "Disallow: " + "\u0085" + CR //
                        + "User-agent: *" + CRLF //
                        + "Disallow: /org/plans.html" + LF //
                        + "Allow: /org/" + CR //
                        + "Allow: /serv" + CRLF //
                        + "Allow: /~mak" + LF //
                        + "Disallow: /" + CRLF;

        BaseRobotRules rules = createRobotRules("Unknown/1.0", mixedEndingsRobotsTxt);
        assertEquals(isAllowed, rules.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.org/", //
                    "False, http://www.fict.org/index.html", //
                    "True, http://www.fict.org/robots.txt", //
                    "False, http://www.fict.org/server.html", //
                    "False, http://www.fict.org/services/fast.html", //
                    "False, http://www.fict.org/services/slow.html", //
                    "False, http://www.fict.org/orgo.gif", //
                    "False, http://www.fict.org/org/about.html", //
                    "False, http://www.fict.org/org/plans.html", //
                    "False, http://www.fict.org/%7Ejim/jim.html", //
                    "False, http://www.fict.org/%7Emak/mak.html" })
    void testRfpCases(boolean isAllowed, String urlStr) {
        // Run through all of the tests that are part of the robots.txt RFP
        // http://www.robotstxt.org/norobots-rfc.txt
        final String rfpExampleRobotsTxt = "# /robots.txt for http://www.fict.org/" + CRLF //
                        + "# comments to webmaster@fict.org" + CRLF //
                        + CRLF //
                        + "User-agent: unhipbot" + CRLF //
                        + "Disallow: /" + CRLF //
                        + "" + CRLF //
                        + "User-agent: webcrawler" + CRLF //
                        + "User-agent: excite" + CRLF //
                        + "Disallow: " + CRLF //
                        + CRLF //
                        + "User-agent: *" + CRLF //
                        + "Disallow: /org/plans.html" + CRLF //
                        + "Allow: /org/" + CRLF //
                        + "Allow: /serv" + CRLF //
                        + "Allow: /~mak" + CRLF //
                        + "Disallow: /" + CRLF;

        BaseRobotRules rules;

        rules = createRobotRules("UnhipBot/0.1", rfpExampleRobotsTxt, false);
        assertEquals(isAllowed, rules.isAllowed(urlStr));

        rules = createRobotRules("WebCrawler/3.0", rfpExampleRobotsTxt, false);
        assertTrue(rules.isAllowed(urlStr));

        rules = createRobotRules("Excite/1.0", rfpExampleRobotsTxt, false);
        assertTrue(rules.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, http://www.fict.org/", //
                    "False, http://www.fict.org/index.html", //
                    "True, http://www.fict.org/robots.txt", //
                    "True, http://www.fict.org/server.html", //
                    "True, http://www.fict.org/services/fast.html", //
                    "True, http://www.fict.org/services/slow.html", //
                    "False, http://www.fict.org/orgo.gif", //
                    "True, http://www.fict.org/org/about.html", //
                    "False, http://www.fict.org/org/plans.html", //
                    "False, http://www.fict.org/%7Ejim/jim.html", //
                    "True, http://www.fict.org/%7Emak/mak.html" })
    void testRfpCases2(boolean isAllowed, String urlStr) {
        // Run through all of the tests that are part of the robots.txt RFP
        // http://www.robotstxt.org/norobots-rfc.txt
        final String rfpExampleRobotsTxt = "# /robots.txt for http://www.fict.org/" + CRLF //
                        + "# comments to webmaster@fict.org" + CRLF //
                        + CRLF //
                        + "User-agent: unhipbot" + CRLF //
                        + "Disallow: /" + CRLF //
                        + "" + CRLF //
                        + "User-agent: webcrawler" + CRLF //
                        + "User-agent: excite" + CRLF //
                        + "Disallow: " + CRLF //
                        + CRLF //
                        + "User-agent: *" + CRLF //
                        + "Disallow: /org/plans.html" + CRLF //
                        + "Allow: /org/" + CRLF //
                        + "Allow: /serv" + CRLF //
                        + "Allow: /~mak" + CRLF //
                        + "Disallow: /" + CRLF;

        BaseRobotRules rules = createRobotRules("Unknown/1.0", rfpExampleRobotsTxt);
        assertEquals(isAllowed, rules.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "False, False, http://www.domain.com/a", //
                    "False, False, http://www.domain.com/a/", //
                    "False, False, http://www.domain.com/a/bloh/foo.html", //
                    "True, True, http://www.domain.com/b", //
                    "False, False, http://www.domain.com/b/a", //
                    "False, False, http://www.domain.com/b/a/index.html", //
                    "True, True, http://www.domain.com/b/b/foo.html", //
                    "True, True, http://www.domain.com/c", //
                    "True, True, http://www.domain.com/c/a", //
                    "True, True, http://www.domain.com/c/a/index.html", //
                    "True, True, http://www.domain.com/c/b/foo.html", //
                    "True, False, http://www.domain.com/d", //
                    "True, False, http://www.domain.com/d/a", //
                    "True, True, http://www.domain.com/e/a/index.html", //
                    "True, True, http://www.domain.com/e/d", //
                    "True, False, http://www.domain.com/e/d/foo.html", //
                    "True, True, http://www.domain.com/e/doh.html", //
                    "True, True, http://www.domain.com/f/index.html", //
                    "True, False, http://www.domain.com/foo/bar/baz.html", //
                    "True, True, http://www.domain.com/f/" })
    void testNutchCases(boolean isAllowed, boolean isMergeAllowed, String urlStr) {
        // Run through the Nutch test cases.
        final String nutchRobotsTxt = "User-Agent: Agent1 #foo" + CR //
                        + "Disallow: /a" + CR //
                        + "Disallow: /b/a" + CR //
                        + "#Disallow: /c" + CR //
                        + "" + CR + "" + CR //
                        + "User-Agent: Agent2 Agent3#foo" + CR //
                        + "User-Agent: Agent4" + CR //
                        + "Disallow: /d" + CR //
                        + "Disallow: /e/d/" + CR //
                        + "" + CR //
                        + "User-Agent: *" + CR //
                        + "Disallow: /foo/bar/" + CR;

        BaseRobotRules rules;

        rules = createRobotRules("Agent1", nutchRobotsTxt);
        assertEquals(isAllowed, rules.isAllowed(urlStr));

        // Note that SimpleRobotRulesParser now merges all matching user agent
        // rules.
        rules = createRobotRules("Agent5,Agent2,Agent1,Agent3,*", nutchRobotsTxt, false);
        assertEquals(isMergeAllowed, rules.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "True, http://www.domain.com/a", //
                    "True, http://www.domain.com/a/", //
                    "True, http://www.domain.com/a/bloh/foo.html", //
                    "True, http://www.domain.com/b", //
                    "True, http://www.domain.com/b/a", //
                    "True, http://www.domain.com/b/a/index.html", //
                    "True, http://www.domain.com/b/b/foo.html", //
                    "True, http://www.domain.com/c", //
                    "True, http://www.domain.com/c/a", //
                    "True, http://www.domain.com/c/a/index.html", //
                    "True, http://www.domain.com/c/b/foo.html", //
                    "False, http://www.domain.com/d", //
                    "False, http://www.domain.com/d/a", //
                    "True, http://www.domain.com/e/a/index.html", //
                    "True, http://www.domain.com/e/d", //
                    "False, http://www.domain.com/e/d/foo.html", //
                    "True, http://www.domain.com/e/doh.html", //
                    "True, http://www.domain.com/f/index.html", //
                    "True, http://www.domain.com/foo/bar/baz.html", //
                    "True, http://www.domain.com/f/" })
    void testNutchCases2(boolean isAllowed, String urlStr) {
        // Run through the Nutch test cases.
        final String nutchRobotsTxt = "User-Agent: Agent1 #foo" + CR //
                        + "Disallow: /a" + CR //
                        + "Disallow: /b/a" + CR //
                        + "#Disallow: /c" + CR //
                        + "" + CR //
                        + "" + CR //
                        + "User-Agent: Agent2 Agent3#foo" + CR //
                        + "User-Agent: Agent4" + CR //
                        + "Disallow: /d" + CR //
                        + "Disallow: /e/d/" + CR //
                        + "" + CR //
                        + "User-Agent: *" + CR //
                        + "Disallow: /foo/bar/" + CR;

        BaseRobotRules rules;

        rules = createRobotRules("Agent2", nutchRobotsTxt, false);
        assertEquals(isAllowed, rules.isAllowed(urlStr));

        rules = createRobotRules("Agent3", nutchRobotsTxt, false);
        assertEquals(isAllowed, rules.isAllowed(urlStr));

        rules = createRobotRules("Agent4", nutchRobotsTxt, false);
        assertEquals(isAllowed, rules.isAllowed(urlStr));
    }

    @ParameterizedTest
    @CsvSource({ "True, http://www.domain.com/a", //
                    "True, http://www.domain.com/a/", //
                    "True, http://www.domain.com/a/bloh/foo.html", //
                    "True, http://www.domain.com/b", //
                    "True, http://www.domain.com/b/a", //
                    "True, http://www.domain.com/b/a/index.html", //
                    "True, http://www.domain.com/b/b/foo.html", //
                    "True, http://www.domain.com/c", //
                    "True, http://www.domain.com/c/a", //
                    "True, http://www.domain.com/c/a/index.html", //
                    "True, http://www.domain.com/c/b/foo.html", //
                    "True, http://www.domain.com/d", //
                    "True, http://www.domain.com/d/a", //
                    "True, http://www.domain.com/e/a/index.html", //
                    "True, http://www.domain.com/e/d", //
                    "True, http://www.domain.com/e/d/foo.html", //
                    "True, http://www.domain.com/e/doh.html", //
                    "True, http://www.domain.com/f/index.html", //
                    "False, http://www.domain.com/foo/bar/baz.html", //
                    "True, http://www.domain.com/f/" })
    void testNutchCases3(boolean isAllowed, String urlStr) {
        // Run through the Nutch test cases.
        final String nutchRobotsTxt = "User-Agent: Agent1 #foo" + CR //
                        + "Disallow: /a" + CR //
                        + "Disallow: /b/a" + CR //
                        + "#Disallow: /c" + CR //
                        + "" + CR //
                        + "" + CR //
                        + "User-Agent: Agent2 Agent3#foo" + CR + "User-Agent: Agent4" + CR //
                        + "Disallow: /d" + CR //
                        + "Disallow: /e/d/" + CR //
                        + "" + CR //
                        + "User-Agent: *" + CR //
                        + "Disallow: /foo/bar/" + CR;

        BaseRobotRules rules = createRobotRules("Agent5", nutchRobotsTxt);
        assertEquals(isAllowed, rules.isAllowed(urlStr));
    }

    @Test
    void testHtmlMarkupInRobotsTxt() {
        final String htmlRobotsTxt = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\"><HTML>\n" //
                        + "<HEAD>\n" //
                        + "<TITLE>/robots.txt</TITLE>\n" //
                        + "</HEAD>\n" //
                        + "<BODY>\n" //
                        + "User-agent: mybot<BR>\n" //
                        + "Disallow: <BR>\n" //
                        + "Crawl-Delay: 10<BR>\n" //
                        + "\n" + "User-agent: *<BR>\n" //
                        + "Disallow: /<BR>\n" //
                        + "Crawl-Delay: 30<BR>\n" //
                        + "\n" //
                        + "</BODY>\n" //
                        + "</HTML>\n";

        BaseRobotRules rules;

        rules = createRobotRules("mybot", htmlRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/index.html"));
        assertEquals(10000, rules.getCrawlDelay());

        rules = createRobotRules("bogusbot", htmlRobotsTxt);
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));
        assertEquals(30000, rules.getCrawlDelay());
    }

    @Test
    void testIgnoreOfHtml() {
        final String htmlFile = "<HTML><HEAD><TITLE>Site under Maintenance</TITLE></HTML>";

        BaseRobotRules rules = createRobotRules("anybot", htmlFile.getBytes(US_ASCII));
        assertTrue(rules.isAllowed("http://www.domain.com/"));
        assertFalse(rules.isDeferVisits());
    }

    @Test
    void testHeritrixCases() {
        final String heritrixRobotsTxt = "User-agent: *\n" //
                        + "Disallow: /cgi-bin/\n" //
                        + "Disallow: /details/software\n" //
                        + "\n" //
                        + "User-agent: denybot\n" //
                        + "Disallow: /\n" //
                        + "\n" //
                        + "User-agent: allowbot1\n" //
                        + "Disallow: \n" //
                        + "\n" //
                        + "User-agent: allowbot2\n" //
                        + "Disallow: /foo\n" //
                        + "Allow: /\n" //
                        + "\n" //
                        + "User-agent: delaybot\n" //
                        + "Disallow: /\n" //
                        + "Crawl-Delay: 20\n" //
                        + "Allow: /images/\n";

        BaseRobotRules rules;
        rules = createRobotRules("Mozilla allowbot1 99.9", heritrixRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/path"));
        assertTrue(rules.isAllowed("http://www.domain.com/"));

        rules = createRobotRules("Mozilla allowbot2 99.9", heritrixRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/path"));
        assertTrue(rules.isAllowed("http://www.domain.com/"));
        assertFalse(rules.isAllowed("http://www.domain.com/foo"));

        rules = createRobotRules("Mozilla denybot 99.9", heritrixRobotsTxt);
        assertFalse(rules.isAllowed("http://www.domain.com/path"));
        assertFalse(rules.isAllowed("http://www.domain.com/"));
        assertEquals(BaseRobotRules.UNSET_CRAWL_DELAY, rules.getCrawlDelay());

        rules = createRobotRules("Mozilla anonbot 99.9", heritrixRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/path"));
        assertFalse(rules.isAllowed("http://www.domain.com/cgi-bin/foo.pl"));

        rules = createRobotRules("Mozilla delaybot 99.9", heritrixRobotsTxt);
        assertEquals(20000, rules.getCrawlDelay());
    }

    @Test
    void testCaseSensitivePaths() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Allow: /AnyPage.html" + CRLF //
                        + "Allow: /somepage.html" + CRLF //
                        + "Disallow: /";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/AnyPage.html"));
        assertFalse(rules.isAllowed("http://www.domain.com/anypage.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/somepage.html"));
        assertFalse(rules.isAllowed("http://www.domain.com/SomePage.html"));
    }

    @Test
    void testEmptyDisallow() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow:";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testEmptyAllow() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Allow:";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testEmptyDisallowLowestPrecedence() {
        String robotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /disallowed/" + CRLF //
                        + "Allow: /allowed/" + CRLF //
                        + "Disallow: ";

        BaseRobotRules rules = createRobotRules("anybot", robotsTxt);
        assertTrue(rules.isAllowed("http://www.example.com/"));
        assertTrue(rules.isAllowed("http://www.example.com/anypage.html"));
        assertFalse(rules.isAllowed("http://www.example.com/disallowed/index.html"));
        assertTrue(rules.isAllowed("http://www.example.com/allowed/index.html"));

        // with merged groups
        robotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /disallowed/" + CRLF //
                        + "Allow: /allowed/" + CRLF //
                        + "User-agent: *" + CRLF //
                        + "Disallow: ";

        rules = createRobotRules("anybot", robotsTxt);
        assertTrue(rules.isAllowed("http://www.example.com/"));
        assertTrue(rules.isAllowed("http://www.example.com/anypage.html"));
        assertFalse(rules.isAllowed("http://www.example.com/disallowed/index.html"));
        assertTrue(rules.isAllowed("http://www.example.com/allowed/index.html"));
    }

    @Test
    void testMultiWildcard() {
        // Make sure we only take the first wildcard entry.
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /" + CRLF //
                        + CRLF //
                        + "User-agent: *" + CRLF //
                        + "Disallow: /";

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testMultiMatches() {
        // Make sure we only take the first record that matches.
        final String simpleRobotsTxt = "User-agent: crawlerbot" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /" + CRLF //
                        + CRLF //
                        + "User-agent: crawler" + CRLF //
                        + "Disallow: /";

        BaseRobotRules rules = createRobotRules("crawlerbot", simpleRobotsTxt);
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testMultiAgentNames() {
        // When there are more than one agent name on a line.
        final String simpleRobotsTxt = "User-agent: crawler1 crawler2" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /";

        BaseRobotRules rules = createRobotRules("crawler2", simpleRobotsTxt, false);
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testMultiWordAgentName() {
        // When the user agent name has a space in it.
        final String simpleRobotsTxt = "User-agent: Download Ninja" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /";

        BaseRobotRules rules = createRobotRules("Download Ninja", simpleRobotsTxt);
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testAgentTokenMatch() {
        // The user-agent token should be matched until the first non-token
        // character,
        // see https://github.com/google/robotstxt/issues/56
        final String simpleRobotsTxt1 = "User-agent: foo/1.2" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /";
        final String simpleRobotsTxt2 = "User-agent: foo bar" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /";

        BaseRobotRules rules = createRobotRules("foo", simpleRobotsTxt1, true);
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));

        rules = createRobotRules("foo", simpleRobotsTxt2, true);
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));
        assertTrue(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testUnsupportedFields() {
        // When we have a new field type that we don't know about.
        final String simpleRobotsTxt = "User-agent: crawler1" + CRLF //
                        + "Disallow: /index.html" + CRLF //
                        + "Allow: /" + CRLF //
                        + "newfield: 234" + CRLF //
                        + "User-agent: crawler2" + CRLF //
                        + "Disallow: /";

        BaseRobotRules rules = createRobotRules("crawler2", simpleRobotsTxt);
        assertFalse(rules.isAllowed("http://www.domain.com/anypage.html"));
    }

    @Test
    void testAcapFields() {
        final String robotsTxt = "acap-crawler: *" + CRLF //
                        + "acap-disallow-crawl: /ultima_ora/";

        SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
        parser.parseContent("url", robotsTxt.getBytes(UTF_8), "text/plain", "foobot");
        assertEquals(0, parser.getNumWarnings());
    }

    @Test
    void testStatusCodeCreation() {
        BaseRobotRules rules;

        SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
        rules = robotParser.failedFetch(HttpURLConnection.HTTP_UNAVAILABLE);
        assertTrue(rules.isDeferVisits());
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));

        rules = robotParser.failedFetch(HttpURLConnection.HTTP_MOVED_PERM);
        assertTrue(rules.isDeferVisits());
        assertFalse(rules.isAllowed("http://www.domain.com/index.html"));

        // All 4xx status codes should result in open access (ala Google)
        // SC_FORBIDDEN
        // SC_NOT_FOUND
        // SC_GONE
        for (int status = 400; status < 420; status++) {
            rules = robotParser.failedFetch(status);
            assertFalse(rules.isDeferVisits());
            assertTrue(rules.isAllowed("http://www.domain.com/index.html"));
        }

        // Calling failedFetch with a good status code should trigger an
        // exception.
        try {
            robotParser.failedFetch(HttpURLConnection.HTTP_OK);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // valid
        }
    }

    @Test
    void testCrawlDelay() {
        final String delayRules1RobotsTxt = "User-agent: bixo" + CR //
                        + "Crawl-delay: 10" + CR //
                        + "User-agent: foobot" + CR //
                        + "Crawl-delay: 20" + CR //
                        + "User-agent: *" + CR //
                        + "Disallow:/baz" + CR;

        BaseRobotRules rules = createRobotRules("bixo", delayRules1RobotsTxt);
        long crawlDelay = rules.getCrawlDelay();
        assertEquals(10000, crawlDelay, "testing crawl delay for agent bixo - rule 1");

        final String delayRules2RobotsTxt = "User-agent: foobot" + CR //
                        + "Crawl-delay: 20" + CR //
                        + "User-agent: *" + CR //
                        + "Disallow:/baz" + CR;

        rules = createRobotRules("bixo", delayRules2RobotsTxt);
        crawlDelay = rules.getCrawlDelay();
        assertEquals(BaseRobotRules.UNSET_CRAWL_DELAY, crawlDelay, "testing crawl delay for agent bixo - rule 2");
    }

    @Test
    void testBigCrawlDelay() {
        final String robotsTxt = "User-agent: *" + CR //
                        + "Crawl-delay: 3600" + CR //
                        + "Disallow:" + CR;

        BaseRobotRules rules = createRobotRules("bixo", robotsTxt);
        assertFalse(rules.isAllowed("http://www.domain.com/"), "disallow all if huge crawl delay");
    }

    @Test
    void testBrokenKrugleRobotsTxtFile() {
        final String krugleRobotsTxt = "User-agent: *" + CR //
                        + "Disallow: /maintenance.html" + CR //
                        + "Disallow: /perl/" + CR //
                        + "Disallow: /cgi-bin/" + CR //
                        + "Disallow: /examples/" + CR //
                        + "Crawl-delay: 3" + CR //
                        + CR //
                        + "User-agent: googlebot" + CR //
                        + "Crawl-delay: 1" + CR //
                        + CR //
                        + "User-agent: qihoobot" + CR //
                        + "Disallow: /";

        // Note: In compliance with RFC 9309, for Googlebot a specific
        // Crawl-delay is set. However, Googlebot is not allowed to crawl any
        // page (same as qihoobot).
        BaseRobotRules rules = createRobotRules("googlebot/2.1", krugleRobotsTxt, false);
        assertEquals(1000L, rules.getCrawlDelay());
        assertFalse(rules.isAllowed("http://www.krugle.com/examples/index.html"));
        assertFalse(rules.isAllowed("http://www.krugle.com/"));

        rules = createRobotRules("googlebot", krugleRobotsTxt, true);
        assertEquals(1000L, rules.getCrawlDelay());
        assertFalse(rules.isAllowed("http://www.krugle.com/examples/index.html"));
        assertFalse(rules.isAllowed("http://www.krugle.com/"));

        rules = createRobotRules("qihoobot", krugleRobotsTxt, true);
        assertEquals(BaseRobotRules.UNSET_CRAWL_DELAY, rules.getCrawlDelay());
        assertFalse(rules.isAllowed("http://www.krugle.com/examples/index.html"));
        assertFalse(rules.isAllowed("http://www.krugle.com/"));

        rules = createRobotRules("anybot", krugleRobotsTxt, true);
        assertEquals(3000L, rules.getCrawlDelay());
        assertFalse(rules.isAllowed("http://www.krugle.com/examples/index.html"));
        assertTrue(rules.isAllowed("http://www.krugle.com/"));
    }

    /** Test selection of Crawl-delay directives in robots.txt (see #114) */
    @Test
    void testSelectCrawlDelayGroup() {
        final String robotsTxt = "User-Agent: bingbot" + CRLF //
                        + "Crawl-delay: 1" + CRLF + CRLF //
                        + "User-Agent: msnbot" + CRLF //
                        + "Crawl-delay: 2" + CRLF + CRLF //
                        + "User-Agent: pinterestbot" + CRLF //
                        + "Crawl-delay: 0.2" + CRLF + CRLF //
                        + "User-agent: *" + CRLF //
                        + "Disallow: /login" + CRLF //
                        + "Sitemap: http://www.example.com/sitemap.xml" + CRLF //
                        + "Disallow: /assets/";

        // test for specific Crawl-delay values but same set of rules
        Map<String, Long> expectedCrawlDelays = Map.of("bingbot", 1000L, "msnbot", 2000L, "anybot", BaseRobotRules.UNSET_CRAWL_DELAY);
        List<String> sitemaps = List.of("http://www.example.com/sitemap.xml");
        for (Entry<String, Long> e : expectedCrawlDelays.entrySet()) {
            BaseRobotRules rules = createRobotRules(e.getKey(), robotsTxt);
            assertEquals(e.getValue(), rules.getCrawlDelay(), "Crawl-delay for " + e.getKey() + " = " + e.getValue());
            assertFalse(rules.isAllowed("http://www.example.com/login"), "Disallow path /login for all agents");
            assertFalse(rules.isAllowed("http://www.example.com/assets/"), "Disallow path /assets for all agents");
            assertTrue(rules.isAllowed("http://www.example.com/"), "Implicitly allowed");
            assertEquals(sitemaps, rules.getSitemaps());
        }
    }

    @Test
    void testRobotsWithUTF8BOM() throws Exception {
        BaseRobotRules rules = createRobotRules("foobot", readFile("/robots/robots-with-utf8-bom.txt"));
        assertFalse(rules.isAllowed("http://www.domain.com/profile"), "Disallow match against *");
    }

    @Test
    void testRobotsWithUTF16LEBOM() throws Exception {
        BaseRobotRules rules = createRobotRules("foobot", readFile("/robots/robots-with-utf16le-bom.txt"));
        assertFalse(rules.isAllowed("http://www.domain.com/profile"), "Disallow match against *");
    }

    @Test
    void testRobotsWithUTF16BEBOM() throws Exception {
        BaseRobotRules rules = createRobotRules("foobot", readFile("/robots/robots-with-utf16be-bom.txt"));
        assertFalse(rules.isAllowed("http://www.domain.com/profile"), "Disallow match against *");
    }

    @Test
    void testFloatingPointCrawlDelay() {
        final String robotsTxt = "User-agent: *" + CR //
                        + "Crawl-delay: 0.5" + CR //
                        + "Disallow:" + CR;

        BaseRobotRules rules = createRobotRules("bixo", robotsTxt);
        assertEquals(500, rules.getCrawlDelay());
    }

    @Test
    void testIgnoringHost() throws Exception {
        BaseRobotRules rules = createRobotRules("foobot", readFile("/robots/www.flot.com-robots.txt"));
        assertFalse(rules.isAllowed("http://www.flot.com/img/"), "Disallow img directory");
    }

    @Test
    void testDirectiveTypos() throws Exception {
        BaseRobotRules rules = createRobotRules("bot1", readFile("/robots/directive-typos-robots.txt"));
        assertFalse(rules.isAllowed("http://domain.com/desallow/"), "desallow");
        assertFalse(rules.isAllowed("http://domain.com/dissalow/"), "dissalow");

        rules = createRobotRules("bot2", readFile("/robots/directive-typos-robots.txt"));
        assertFalse(rules.isAllowed("http://domain.com/useragent/"), "useragent");

        rules = createRobotRules("bot3", readFile("/robots/directive-typos-robots.txt"));
        assertFalse(rules.isAllowed("http://domain.com/useg-agent/"), "useg-agent");

        rules = createRobotRules("bot4", readFile("/robots/directive-typos-robots.txt"));
        assertFalse(rules.isAllowed("http://domain.com/useragent-no-colon/"), "useragent-no-colon");
    }

    @Test
    void testFormatErrors() throws Exception {
        BaseRobotRules rules = createRobotRules("bot1", readFile("/robots/format-errors-robots.txt"));
        assertFalse(rules.isAllowed("http://domain.com/whitespace-before-colon/"), "whitespace-before-colon");
        assertFalse(rules.isAllowed("http://domain.com/no-colon/"), "no-colon");

        rules = createRobotRules("bot2", readFile("/robots/format-errors-robots.txt"));
        assertFalse(rules.isAllowed("http://domain.com/no-colon-useragent/"), "no-colon-useragent");

        rules = createRobotRules("bot3", readFile("/robots/format-errors-robots.txt"));
        assertTrue(rules.isAllowed("http://domain.com/whitespace-before-colon/"), "whitespace-before-colon");
    }

    // See http://www.conman.org/people/spc/robots2.html
    @Test
    void testExtendedStandard() throws Exception {
        SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
        robotParser.parseContent(FAKE_ROBOTS_URL, readFile("/robots/extended-standard-robots.txt"), "text/plain", "foobot");
        assertEquals(0, robotParser.getNumWarnings(), "Zero warnings with expended directives");
    }

    @Test
    void testSitemap() throws Exception {
        BaseRobotRules rules = createRobotRules("bot1", readFile("/robots/sitemap-robots.txt"));
        assertEquals(3, rules.getSitemaps().size(), "Found sitemap");
        // check that the last one is not lowercase only
        String url = rules.getSitemaps().get(2);
        boolean lowercased = url.equals(url.toLowerCase(Locale.ROOT));
        assertFalse(lowercased, "Sitemap case check");
    }

    @Test
    void testRelativeSitemap() throws Exception {
        BaseRobotRules rules = createRobotRules("bot1", readFile("/robots/relative-sitemap-robots.txt"));
        assertEquals(1, rules.getSitemaps().size(), "Found sitemap");
    }

    @Test
    void testSitemapInvalidBaseUrl() {
        // test https://github.com/crawler-commons/crawler-commons/issues/240
        // - should handle absolute sitemap URL even if base URL isn't valid

        final String simpleRobotsTxt = "Sitemap: https://www.example.com/sitemap.xml";

        SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
        BaseRobotRules rules = robotParser.parseContent("example.com", simpleRobotsTxt.getBytes(UTF_8), "text/plain", "a");

        assertEquals(1, rules.getSitemaps().size());
        assertEquals("https://www.example.com/sitemap.xml", rules.getSitemaps().get(0));
        assertEquals(1, rules.getSitemaps().size(), "Found sitemap");
    }

    @Test
    void testSitemapDedup() throws Exception {
        BaseRobotRules rules = createRobotRules("bot1", readFile("/robots/sitemap-robots-dedup.txt"));
        assertEquals(1, rules.getSitemaps().size(), "Sitemaps deduped");
    }

    @Test
    void testManyUserAgents() throws Exception {
        BaseRobotRules rules = createRobotRules("wget", readFile("/robots/many-user-agents.txt"));
        assertFalse(rules.isAllowed("http://domain.com/"), "many-user-agents");

        rules = createRobotRules("mysuperlongbotnamethatmatchesnothing", readFile("/robots/many-user-agents.txt"));
        assertTrue(rules.isAllowed("http://domain.com/"), "many-user-agents");
        assertFalse(rules.isAllowed("http://domain.com/bot-trap/"), "many-user-agents");
    }

    @Test
    void testManyUserAgentsMergeRules() throws Exception {
        BaseRobotRules rules = createRobotRules("wget", readFile("/robots/merge-rules.txt"));
        assertTrue(rules.isAllowed("http://domain.com/foo/bar/bar.txt"), "merge-rules");
        assertFalse(rules.isAllowed("http://domain.com/tmp/bar/bar.txt"), "merge-rules");
        assertTrue(rules.isAllowed("http://domain.com/chk/bar/bar.txt"), "merge-rules");
    }

    @Test
    void testPrecedenceOfRules() throws Exception {
        BaseRobotRules rules = createRobotRules("wget", readFile("/robots/precedence-of-rules.txt"));

        assertTrue(rules.isAllowed("http://domain.com/"), "precedence-of-rules");
        assertTrue(rules.isAllowed("http://domain.com/foo/bar"), "precedence-of-rules");
        assertFalse(rules.isAllowed("http://domain.com/bar/foo"), "precedence-of-rules");

        rules = createRobotRules("testy", readFile("/robots/precedence-of-rules.txt"));

        assertTrue(rules.isAllowed("http://domain.com/"), "precedence-of-rules");
        assertTrue(rules.isAllowed("http://domain.com/foo/bar"), "precedence-of-rules");
        assertFalse(rules.isAllowed("http://domain.com/foo/blue"), "precedence-of-rules");
        assertFalse(rules.isAllowed("http://domain.com/foo/"), "precedence-of-rules");
        assertFalse(rules.isAllowed("http://domain.com/foo/tar"), "precedence-of-rules");
    }

    @Test
    void testMalformedPathInRobotsFile() throws Exception {
        BaseRobotRules rules = createRobotRules("bot1", readFile("/robots/malformed-path.txt"));
        assertFalse(rules.isAllowed("http://en.wikipedia.org/wiki/Wikipedia_talk:Mediation_Committee/"), "Disallowed URL");
        assertTrue(rules.isAllowed("http://en.wikipedia.org/wiki/"), "Regular URL");
    }

    @Test
    void testDOSlineEndings() throws Exception {
        BaseRobotRules rules = createRobotRules("bot1", readFile("/robots/dos-line-endings.txt"));
        assertTrue(rules.isAllowed("http://ford.com/"), "Allowed URL");
        assertEquals(1000L, rules.getCrawlDelay());
    }

    @Test
    void testAmazonRobotsWithWildcards() throws Exception {
        BaseRobotRules rules = createRobotRules("anybot", readFile("/robots/wildcards.txt"));
        assertFalse(rules.isAllowed("http://www.fict.com/wishlist/bogus"));
        assertTrue(rules.isAllowed("http://www.fict.com/wishlist/universal/page"));
        assertFalse(rules.isAllowed("http://www.fict.com/anydirectoryhere/gcrnsts"));
    }

    @Test
    void testAllowBeforeDisallow() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Disallow: /fish" + CRLF //
                        + "Allow: /fish" + CRLF;

        BaseRobotRules rules = createRobotRules("anybot", simpleRobotsTxt);

        assertTrue(rules.isAllowed("http://www.fict.com/fish"));
    }

    @Test
    void testSpacesInMultipleUserAgentNames() {
        final String simpleRobotsTxt = "User-agent: One, Two, Three" + CRLF //
                        + "Disallow: /" + CRLF //
                        + "" + CRLF //
                        + "User-agent: *" + CRLF //
                        + "Allow: /" + CRLF;

        BaseRobotRules rules = createRobotRules("One", simpleRobotsTxt);
        assertFalse(rules.isAllowed("http://www.fict.com/fish"));

        rules = createRobotRules("Two", simpleRobotsTxt, false);
        assertFalse(rules.isAllowed("http://www.fict.com/fish"));

        rules = createRobotRules("Three", simpleRobotsTxt, false);
        assertFalse(rules.isAllowed("http://www.fict.com/fish"));

        rules = createRobotRules("anybot", simpleRobotsTxt);
        assertTrue(rules.isAllowed("http://www.fict.com/fish"));
    }

    @Test
    void testMatchingOfUserAgentNamesIndependentFromSequentialOrder() {
        String url = "https://example.com/foo/bar";

        /*
         * URL is allowed for "crawler" but disallowed for "crawlerbot"
         * independent from sequential ordering of rules
         */

        String robotsTxtBlock1 = "User-agent: crawlerbot\n" //
                        + "Disallow: /foo/bar\n";
        String robotsTxtBlock2 = "User-agent: crawler\n" //
                        + "Allow: /foo/bar\n";

        String[] robotsTxts = new String[2];
        robotsTxts[0] = robotsTxtBlock1 + "\n" + robotsTxtBlock2;
        robotsTxts[1] = robotsTxtBlock2 + "\n" + robotsTxtBlock1;

        BaseRobotRules rules;
        for (String robotsTxt : robotsTxts) {
            rules = createRobotRules("crawlerbot", robotsTxt, true);
            assertFalse(rules.isAllowed(url));
            rules = createRobotRules("crawler", robotsTxt, true);
            assertTrue(rules.isAllowed(url));
        }
    }

    @Test
    void testMatchingUserAgentNamesHttpHeader() {
        /*
         * Tests examples from #192 - matching user-agent directives containing
         * the strings sent by the crawler in the <i>User-Agent</i> HTTP request
         * header.
         */
        String url = "http://example.org/";
        String robotsTxt = "User-agent: Mozilla/5.0 (compatible; Butterfly/1.0; +http://labs.topsy.com/butterfly/) Gecko/2009032608 Firefox/3.0.8" + LF //
                        + "Disallow: /";
        BaseRobotRules rules;

        // exact user-agent matching: rule does not apply
        String robot = "butterfly";
        rules = createRobotRules(robot, robotsTxt, true);
        assertTrue(rules.isAllowed(url));

        // prefix user-agent matching: rule does not apply anyway because
        // "butterfly/1.0" (from robots.txt) is not a prefix of butterfly (from
        // configuration / API call)
        rules = createRobotRules(robot, robotsTxt, false);
        assertTrue(rules.isAllowed(url));

        // prefix user-agent matching: user-agent is matched if a token expected
        // in the user-agent line is included literally (but lower-case)
        String[] butterflyRobots = { "butterfly", "butterfly/1.0", "butterfly/1.0;" };
        rules = createRobotRules(butterflyRobots, robotsTxt, false);
        assertFalse(rules.isAllowed(url));

        // exact user-agent matching: user-agent is matched if the entire
        // user-agent line is passed as one robot name (lower-case)
        String[] butterflyRobotsFullList = { "butterfly", "butterfly/1.0", "mozilla/5.0 (compatible; butterfly/1.0; +http://labs.topsy.com/butterfly/) gecko/2009032608 firefox/3.0.8" };
        rules = createRobotRules(butterflyRobotsFullList, robotsTxt, true);
        assertFalse(rules.isAllowed(url));

        // exact user-agent matching: different user-agents which share only one
        // token (here "mozilla/5.0" or "(compatible;" are not matched
        String[] myRobots = { "mybot", "mybot/1.0", "mozilla/5.0 (compatible; mybot/1.0)" };
        rules = createRobotRules(myRobots, robotsTxt, true);
        assertTrue(rules.isAllowed(url));
    }

    // https://github.com/crawler-commons/crawler-commons/issues/112
    @Test
    void testSitemapAtEndOfFile() {
        final String simpleRobotsTxt = "User-agent: a" + CRLF //
                        + "Disallow: /content/dam/" + CRLF //
                        + CRLF //
                        + "User-agent: b" + CRLF //
                        + "Disallow: /content/dam/" + CRLF //
                        + CRLF //
                        + "User-agent: c" + CRLF //
                        + "Disallow: /content/dam/" + CRLF //
                        + CRLF + CRLF //
                        + "Sitemap: https://wwwfoocom/sitemapxml";

        BaseRobotRules rules = createRobotRules("a", simpleRobotsTxt);
        assertEquals(1, rules.getSitemaps().size());
        assertEquals("https://wwwfoocom/sitemapxml", rules.getSitemaps().get(0));
    }

    @Test
    void testOverrideUserAgentMatcher() {
        @SuppressWarnings("serial")
        BaseRobotsParser myRobotsParser = new SimpleRobotRulesParser() {
            @Override
            protected boolean userAgentProductTokenPartialMatch(String userAgent, Collection<String> targetTokens) {
                return userAgent.toLowerCase(Locale.ROOT).contains("go!zilla");
            }
        };
        final String robotsTxt = "User-agent: Go!zilla/2.0" + CRLF //
                        + "Allow: /gozilla/" + CRLF //
                        + "Disallow: /";
        BaseRobotRules rules = myRobotsParser.parseContent(FAKE_ROBOTS_URL, robotsTxt.getBytes(UTF_8), "text/plain", List.of("go!zilla"));
        assertTrue(rules.isAllowed("https://example.org/gozilla/page.html"));
        assertFalse(rules.isAllowed("https://example.org/other/page.html"));
    }

    @Test
    void testExamplesRobotsTxtRFC9309() throws Exception {
        byte[] robotstxt = readFile("/robots/rfc9309-example-simple-robots.txt");

        BaseRobotRules rules = createRobotRules("foobot", robotstxt);
        assertEquals(3, ((SimpleRobotRules) rules).getRobotRules().size());
        assertTrue(rules.isAllowed("https://example.org/example/page.html"));
        assertTrue(rules.isAllowed("https://example.org/example/allowed.gif"));
        assertFalse(rules.isAllowed("https://example.org/"));
        assertFalse(rules.isAllowed("https://example.org/path/index.html"));

        rules = createRobotRules("barbot", robotstxt);
        assertEquals(1, ((SimpleRobotRules) rules).getRobotRules().size());
        assertTrue(rules.isAllowed("https://example.org/"));
        assertFalse(rules.isAllowed("https://example.org/example/page.html"));
        rules = createRobotRules("bazbot", robotstxt);
        assertEquals(1, ((SimpleRobotRules) rules).getRobotRules().size());
        assertTrue(rules.isAllowed("https://example.org/"));
        assertFalse(rules.isAllowed("https://example.org/example/page.html"));

        rules = createRobotRules("quxbot", robotstxt);
        assertEquals(0, ((SimpleRobotRules) rules).getRobotRules().size());
        assertTrue(rules.isAllowed("https://example.org/"));
        assertTrue(rules.isAllowed("https://example.org/example/page.html"));

        rules = createRobotRules("anyotherbot", robotstxt);
        assertEquals(3, ((SimpleRobotRules) rules).getRobotRules().size());
        assertTrue(rules.isAllowed("https://example.org/publications/doc1.html"));
        assertFalse(rules.isAllowed("https://example.org/example/page.html"));
        assertFalse(rules.isAllowed("https://example.org/example.gif"));
        assertTrue(rules.isAllowed("https://example.org/"), "implicitly allowed");

        robotstxt = readFile("/robots/rfc9309-example-longest-match-robots.txt");
        rules = createRobotRules("foobot", robotstxt);
        assertTrue(rules.isAllowed("https://example.org/example/page/"));
        assertTrue(rules.isAllowed("https://example.org/example/page/index.html"));
        assertFalse(rules.isAllowed("https://example.org/example/page/disallowed.gif"));
        assertTrue(rules.isAllowed("https://example.org/"), "implicitly allowed");

        robotstxt = readFile("/robots/rfc9309-example-rule-group-merging.txt");
        rules = createRobotRules("examplebot", robotstxt);
        assertEquals(3, ((SimpleRobotRules) rules).getRobotRules().size());
        assertFalse(rules.isAllowed("https://example.org/foo"));
        assertFalse(rules.isAllowed("https://example.org/bar"));
        assertFalse(rules.isAllowed("https://example.org/baz"));
        assertTrue(rules.isAllowed("https://example.org/"), "implicitly allowed");

        rules = createRobotRules("anyotherbot", robotstxt);
        assertEquals(2, ((SimpleRobotRules) rules).getRobotRules().size());
        assertFalse(rules.isAllowed("https://example.org/foo"));
        assertFalse(rules.isAllowed("https://example.org/bar"));
        assertTrue(rules.isAllowed("https://example.org/baz"));
        assertTrue(rules.isAllowed("https://example.org/"), "implicitly allowed");

        rules = createRobotRules("bazbot", robotstxt);
        assertEquals(1, ((SimpleRobotRules) rules).getRobotRules().size());
        assertTrue(rules.isAllowed("https://example.org/foo"));
        assertTrue(rules.isAllowed("https://example.org/bar"));
        assertFalse(rules.isAllowed("https://example.org/baz"));
        assertTrue(rules.isAllowed("https://example.org/"), "implicitly allowed");
    }

    @Test
    void testAPIemptyUserAgentList() {
        final String simpleRobotsTxt = "User-agent: *" + CRLF //
                        + "Allow: /allowed/" + CRLF //
                        + "Disallow: /" + CRLF //
                        + "User-agent: allowedbot" + CRLF //
                        + "Allow: /";

        /*
         * verify that the wildcard user-agent rules are selected if an empty
         * list of user-agents is passed
         */
        BaseRobotRules rules = createRobotRules(Set.of(), simpleRobotsTxt, true);
        assertTrue(rules.isAllowed("https://www.example.com/allowed/page.html"));
        assertFalse(rules.isAllowed("https://www.example.com/"));

        rules = createRobotRules(Set.of("anybot"), simpleRobotsTxt, true);
        assertTrue(rules.isAllowed("https://www.example.com/allowed/page.html"));
        assertFalse(rules.isAllowed("https://www.example.com/"));

        rules = createRobotRules(Set.of("allowedbot"), simpleRobotsTxt, true);
        assertTrue(rules.isAllowed("https://www.example.com/"));
    }

    @Test
    void testAPIsanitizeUserAgentList() throws Exception {
        byte[] robotstxt = readFile("/robots/rfc9309-example-simple-robots.txt");

        // verify that an exception is thrown if the provided user-agent tokens
        // include a invalid elements
        assertThrows(IllegalArgumentException.class, () -> createRobotRules(Set.of("FOOBOT"), robotstxt, true));
        assertThrows(IllegalArgumentException.class, () -> createRobotRules(Set.of("*"), robotstxt, true));

        Collection<String> robotNames = SimpleRobotRulesParser.sanitizeRobotNames(Set.of("FOOBOT", "*"));
        BaseRobotRules rules = createRobotRules(robotNames, robotstxt, true);
        assertEquals(3, ((SimpleRobotRules) rules).getRobotRules().size());
        assertTrue(rules.isAllowed("https://example.org/example/page.html"));
        assertTrue(rules.isAllowed("https://example.org/example/allowed.gif"));
        assertFalse(rules.isAllowed("https://example.org/"));
        assertFalse(rules.isAllowed("https://example.org/path/index.html"));
    }

    private byte[] readFile(String filename) throws Exception {
        byte[] bigBuffer = new byte[100000];
        InputStream is = SimpleRobotRulesParserTest.class.getResourceAsStream(filename);
        int len = is.read(bigBuffer);
        return Arrays.copyOf(bigBuffer, len);
    }
}
