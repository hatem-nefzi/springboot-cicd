file://<WORKSPACE>/src/main/java/com/example/demo/HelloController.java
### java.util.NoSuchElementException: next on empty iterator

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
offset: 478
uri: file://<WORKSPACE>/src/main/java/com/example/demo/HelloController.java
text:
```scala
package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
//hello world sssssssssssssssss
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello World from Backend!";
    }

    @GetMapping("/time")
    public String getT@@ime() {
        return "Current server time is: " + LocalDateTime.now();
    }

    @GetMapping("/greet")
    public String greetUser(@RequestParam(value = "name", defaultValue = "User") String name) {
        return "Hello, " + name + "!";
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