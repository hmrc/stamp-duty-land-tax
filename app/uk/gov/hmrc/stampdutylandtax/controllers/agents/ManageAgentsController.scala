/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.stampdutylandtax.controllers.agents

import models.agent.*
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ActionBuilder, AnyContent, ControllerComponents}
import service.ManageAgentsService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ManageAgentsController @Inject()(
  cc: ControllerComponents,
  service: ManageAgentsService,
  auth: IdentifierAction
)(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getSdltOrganisation(storn: String): Action[AnyContent] = auth.async { implicit request =>
    

    service.getSdltOrganisation(storn) map { sdltOrganisation =>
      Ok(Json.toJson(
        sdltOrganisation
      ))
    } recover {
      case u: UpstreamErrorResponse =>
        Status(u.statusCode)(Json.obj("message" -> u.message))
      case t: Throwable =>
        logger.error("[ManageAgentsController][getSdltOrganisation] failed", t)
        InternalServerError(Json.obj("message" -> "Unexpected error"))
    }
  }

  def deletePredefinedAgent: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body
      .validate[DeletePredefinedAgentRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .deletePredefinedAgent(body)
            .map { result =>
              Ok(Json.toJson(result))
            }
            .recover {
              case u: UpstreamErrorResponse =>
                Status(u.statusCode)(Json.obj("message" -> u.message))
              case t: Throwable =>
                logger.error("[ManageAgentsController][deletePredefinedAgent] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
    }

  def submitAgentDetails: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[CreatePredefinedAgentRequest].fold(
      invalid => Future.successful(BadRequest(Json.obj("message" -> s"Invalid payload: $invalid"))),
      payload =>
        service.submitAgentDetails(payload) map { submissionResponse =>
          Ok(Json.toJson(
            submissionResponse
          ))
        } recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case t: Throwable =>
            logger.error("[ManageAgentsController][submitAgentDetails] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    )
  }

  def updateAgentDetails: Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body.validate[UpdatePredefinedAgentRequest].fold(
      invalid => Future.successful(BadRequest(Json.obj("message" -> s"Invalid payload: $invalid"))),
      payload =>
        service.updateAgentDetails(payload) map { response =>
          Ok(Json.toJson(
            response
          ))
        } recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case t: Throwable =>
            logger.error("[ManageAgentsController][updateAgentDetails] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    )
  }
}
