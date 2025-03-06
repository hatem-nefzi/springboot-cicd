error id: `<none>`.
file://<WORKSPACE>/src/test/scala/com/example/demo/HelloControllerSimulation.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
|empty definition using fallback
non-local guesses:
	 -io/gatling/core/Predef.
	 -io/gatling/core/Predef#
	 -io/gatling/core/Predef().
	 -io/gatling/http/Predef.
	 -io/gatling/http/Predef#
	 -io/gatling/http/Predef().
	 -scala/concurrent/duration.
	 -scala/concurrent/duration#
	 -scala/concurrent/duration().
	 -.
	 -#
	 -().
	 -scala/Predef.
	 -scala/Predef#
	 -scala/Predef().

Document text:

```scala
package com.example.demo

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class HelloControllerSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8081") // Update with your app's URL
    .acceptHeader("application/json")

  val scn = scenario("HelloControllerSimulation")
    .exec(
      http("Get Hello")
        .get("/hello")
        .check(status.is(200))
    )
    .pause(1.second)
    .exec(
      http("Get Time")
        .get("/time")
        .check(status.is(200))
    )
    .pause(1.second)
    .exec(
      http("Greet User")
        .get("/greet?name=Gatling")
        .check(status.is(200))
    )

  setUp(
    scn.inject(
      atOnceUsers(10) // Simulate 10 users at once
    )
  ).protocols(httpProtocol)
}
```

#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.