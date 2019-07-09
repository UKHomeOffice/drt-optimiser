package optimiser

import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class OptimiserSpec extends FlatSpec with Matchers {
  "A call to crunch some workload" should "produce some desk recommendations" in {
    val workload = 0 until 1440 map (_ => Math.random() * 50)
    val minDesks = List.fill[Int](1440)(2)
    val maxDesks = List.fill[Int](1440)(12)
    val tryDeskRecs = Optimiser.optimise(workload, minDesks, maxDesks, OptimizerConfig(15))

    tryDeskRecs match {
      case Success(x) => println(s"yay")
      case Failure(t) => println(s"noo $t")
    }

    val success = tryDeskRecs.isSuccess
    val correctMinutes = tryDeskRecs.get.desks.length == 1440

    success && correctMinutes should be (true)
  }

  "A call to simulate some workload with desks specified" should "wait times" in {
    val workload = 0 until 1440 map (_ => Math.random() * 50)
    val openDesks = List.fill[Int](1440)(2)
    val tryDeskRecs = Optimiser.simulate(workload, openDesks, OptimizerConfig(15))

    tryDeskRecs.length should be (1440)
  }
}
