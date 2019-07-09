package optimiser

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit val desksAndWaitsJsonFormat: RootJsonFormat[DesksAndWaits] = jsonFormat2(DesksAndWaits)
  implicit val workloadToOptimiseJsonFormat: RootJsonFormat[WorkloadToOptimise] = jsonFormat4(WorkloadToOptimise)
  implicit val workloadToSimulateJsonFormat: RootJsonFormat[WorkloadToSimulate] = jsonFormat3(WorkloadToSimulate)
}
