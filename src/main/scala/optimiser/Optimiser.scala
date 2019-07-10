package optimiser

import java.io.InputStream

import javax.script.{ScriptEngine, ScriptEngineManager}
import org.renjin.sexp.{DoubleVector, IntVector}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.IndexedSeq
import scala.util.{Failure, Success, Try}

case class OptimizerConfig(sla: Int)

final case class DesksAndWaits(desks: Seq[Int], waits: Seq[Int])
final case class WorkloadToOptimise(workloads: Seq[Double], minDesks: Seq[Int], maxDesks: Seq[Int], sla: Int, description: String)
final case class WorkloadToSimulate(workloads: Seq[Double], desks: Seq[Int], sla: Int, description: String)

object Optimiser {
  val log: Logger = LoggerFactory.getLogger(getClass)
  lazy val manager: ScriptEngineManager = new ScriptEngineManager()
  lazy val rEngine: ScriptEngine = manager.getEngineByName("Renjin")

  def optimise(workloadAndDesks: WorkloadToOptimise): DesksAndWaits = {
    log.info(s"Optimising ${workloadAndDesks.description}")
    val tryCrunchRes = Try {
      loadOptimiserScript
      initialiseWorkloads(workloadAndDesks.workloads)

      rEngine.put("xmax", workloadAndDesks.maxDesks.toArray)
      rEngine.put("xmin", workloadAndDesks.minDesks.toArray)
      rEngine.put("sla", workloadAndDesks.sla)
      rEngine.put("adjustedSla", 0.75d * workloadAndDesks.sla)
      rEngine.put("weight_churn", 50)
      rEngine.put("weight_pax", 0.05)
      rEngine.put("weight_staff", 3)
      rEngine.put("weight_sla", 10)

      val adjustedXMax = if (workloadAndDesks.workloads.length > 60) {
        rEngine.eval("rollingfairxmax <- rolling.fair.xmax(w, xmin=xmin, block.size=5, sla=adjustedSla, target.width=60, rolling.buffer=120)")
        val fairXmax = rEngine.eval("rollingfairxmax").asInstanceOf[DoubleVector]
        fairXmax.toIntArray.toSeq.zip(workloadAndDesks.maxDesks).map { case (fair, orig) => List(fair, orig).min }
      } else workloadAndDesks.maxDesks

      rEngine.put("adjustedXMax", adjustedXMax.toArray)

      rEngine.eval("optimised <- optimise.win(w, xmin=xmin, xmax=adjustedXMax, sla=sla, weight.churn=weight_churn, weight.pax=weight_pax, weight.staff=weight_staff, weight.sla=weight_sla)")

      val deskRecs = rEngine.eval("optimised").asInstanceOf[DoubleVector]
      val deskRecsScala = (0 until deskRecs.length()) map deskRecs.getElementAsInt
      DesksAndWaits(deskRecsScala, runSimulation(deskRecsScala, "optimised", workloadAndDesks.sla))
    }

    tryCrunchRes match {
      case Success(desksAndWaits) => desksAndWaits
      case Failure(t) =>
        log.error("Failed to optimise workload", t)
        DesksAndWaits(Seq(), Seq())
    }
  }

  def simulate(workloadToSimulate: WorkloadToSimulate): Seq[Int] = {
    loadOptimiserScript
    log.info(s"Setting ${workloadToSimulate.workloads.length} workloads & ${workloadToSimulate.desks.length} desks")
    initialiseWorkloads(workloadToSimulate.workloads)
    initialiseDesks("desks", workloadToSimulate.desks)
    runSimulation(workloadToSimulate.desks, "desks", workloadToSimulate.sla).toList
  }

  def runSimulation(deskRecsScala: Seq[Int], desks: String, sla: Int): Seq[Int] = {
    rEngine.put("sla", sla)
    rEngine.eval("processed <- process.work(w, " + desks + ", sla, 0)")

    val waitRV = rEngine.eval(s"processed$$wait").asInstanceOf[IntVector]
    val waitTimes: IndexedSeq[Int] = (0 until waitRV.length()) map waitRV.getElementAsInt

    waitTimes
  }

  def initialiseWorkloads(workloads: Seq[Double]): Unit = {
    rEngine.put("w", workloads.toArray)
  }

  def initialiseDesks(varName: String, desks: Seq[Int]): Unit = {
    rEngine.put(varName, desks.toArray)
  }

  def loadOptimiserScript: AnyRef = {
    if (rEngine == null) throw new scala.RuntimeException("Couldn't load Renjin script engine on the classpath")
    val asStream: InputStream = getClass.getResourceAsStream("/optimisation-v6.R")

    val optimiserScript = scala.io.Source.fromInputStream(asStream)
    rEngine.eval(optimiserScript.bufferedReader())
  }

}
