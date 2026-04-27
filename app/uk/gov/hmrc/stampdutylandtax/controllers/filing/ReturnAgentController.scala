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

package uk.gov.hmrc.stampdutylandtax.controllers.filing

import models.filing.*
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import service.filing.ReturnAgentService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ReturnAgentController @Inject() (
    cc: ControllerComponents,
    service: ReturnAgentService,
    auth: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createReturnAgent(): Action[JsValue] = auth.async(parse.json) {
    implicit request =>
      request.body
        .validate[CreateReturnAgentRequest]
        .fold(
          errs =>
            Future.successful(
              BadRequest(
                Json.obj(
                  "message" -> "Invalid payload",
                  "errors" -> JsError.toJson(errs)
                )
              )
            ),
          body =>
            service
              .createReturnAgent(body)
              .map { result =>
                Created(Json.toJson(result))
              }
              .recover { case t =>
                logger.error("[createReturnAgent] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
  }

  def updateReturnAgent(): Action[JsValue] = auth.async(parse.json) {
    implicit request =>
      request.body
        .validate[UpdateReturnAgentRequest]
        .fold(
          errs =>
            Future.successful(
              BadRequest(
                Json.obj(
                  "message" -> "Invalid payload",
                  "errors" -> JsError.toJson(errs)
                )
              )
            ),
          body =>
            service
              .updateReturnAgent(body)
              .map { result =>
                Created(Json.toJson(result))
              }
              .recover { case t =>
                logger.error("[updateReturnAgent] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
  }

  def deleteReturnAgent(): Action[JsValue] = auth.async(parse.json) {
    implicit request =>
      request.body
        .validate[DeleteReturnAgentRequest]
        .fold(
          errs =>
            Future.successful(
              BadRequest(
                Json.obj(
                  "message" -> "Invalid payload",
                  "errors" -> JsError.toJson(errs)
                )
              )
            ),
          body =>
            service
              .deleteReturnAgent(body)
              .map { result =>
                Created(Json.toJson(result))
              }
              .recover { case t =>
                logger.error("[deleteReturnAgent] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
  }

}
