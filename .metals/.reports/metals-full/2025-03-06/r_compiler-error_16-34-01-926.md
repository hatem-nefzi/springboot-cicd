file://<WORKSPACE>/src/test/java/com/example/tests/ApiTests.java
### java.util.NoSuchElementException: next on empty iterator

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
offset: 69
uri: file://<WORKSPACE>/src/test/java/com/example/tests/ApiTests.java
text:
```scala
package com.example.tests;

import com.microsoft.playwright.*;
import@@ org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiTests {

    static Playwright playwright;
    static APIRequestContext request;

    @BeforeAll
    static void setup() {
        playwright = Playwright.create();
        request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL("http://localhost:8081")); // Update port if needed
    }

    @AfterAll
    static void teardown() {
        request.dispose();
        playwright.close();
    }

    @Test
    void testHelloEndpoint() {
        APIResponse response = request.get("/hello");
        assertEquals(200, response.status());
        assertEquals("Hello World from Backend!", response.text());
    }

    @Test
    void testTimeEndpoint() {
        APIResponse response = request.get("/time");
        assertEquals(200, response.status());
        assertTrue(response.text().contains("Current server time is:"));
    }

    @Test
    void testGreetEndpoint() {
        APIResponse response = request.get("/greet?name=Hatem");
        assertEquals(200, response.status());
        assertEquals("Hello, Hatem!", response.text());
    }

    @Test
    void testGreetEndpointDefaultValue() {
        APIResponse response = request.get("/greet");
        assertEquals(200, response.status());
        assertEquals("Hello, User!", response.text()); // Default name should be "User"
    }
}

```



#### Error stacktrace:

```
scala.collection.Iterator$$anon$19.next(Iterator.scala:973)
	scala.collection.Iterator$$anon$19.next(Iterator.scala:971)
	scala.collection.mutable.MutationTracker$CheckedIterator.next(MutationTracker.scala:76)
	scala.collection.IterableOps.head(Iterable.scala:222)
	scala.collection.IterableOps.head$(Iterable.scala:222)
	scala.collection.AbstractIterable.head(Iterable.scala:935)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:164)
	dotty.tools.pc.MetalsDriver.run(MetalsDriver.scala:45)
	dotty.tools.pc.HoverProvider$.hover(HoverProvider.scala:40)
	dotty.tools.pc.ScalaPresentationCompiler.hover$$anonfun$1(ScalaPresentationCompiler.scala:376)
```
#### Short summary: 

java.util.NoSuchElementException: next on empty iterator