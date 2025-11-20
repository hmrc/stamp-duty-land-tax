package models.agent

import play.api.libs.json.{Json, OFormat}

case class UpdateAgentDetailsRequest (
                                        storn                : String,
                                        agentReferenceNumber : String,
                                        agentName            : String,
                                        agentId              : Option[String],
                                        addressLine1         : Option[String],
                                        addressLine2         : Option[String],
                                        addressLine3         : Option[String],
                                        addressLine4         : Option[String],
                                        postcode             : Option[String],
                                        phone                : Option[String],
                                        email                : Option[String]
                                     )
object UpdateAgentDetailsRequest {
  implicit val format: OFormat[UpdateAgentDetailsRequest] = Json.format[UpdateAgentDetailsRequest]
}
