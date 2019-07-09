package optimiser

import akka.actor.{Actor, ActorLogging, Props}

object OptimiserActor {
  final case class OptimiseWorkload(workloadToOptimise: WorkloadToOptimise)
  final case class SimulateWorkload(workloadToSimulate: WorkloadToSimulate)

  def props: Props = Props[OptimiserActor]
}

class OptimiserActor extends Actor with ActorLogging {
  import OptimiserActor._

  def receive: Receive = {
    case OptimiseWorkload(workloadToOptimise) =>
      sender() ! Optimiser.optimise(workloadToOptimise)
    case SimulateWorkload(workloadToSimulate) =>
      sender() ! Optimiser.simulate(workloadToSimulate)
  }
}
