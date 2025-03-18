package com.example.demo

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class HelloControllerSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://192.168.1.16:8081") //change this back to "http://192.168.1.2:8081" when at home
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
      atOnceUsers(20) // Simulate 20 users at once
    )
  ).protocols(httpProtocol)
}
