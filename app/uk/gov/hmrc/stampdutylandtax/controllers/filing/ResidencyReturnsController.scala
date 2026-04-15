/*
 * Copyright 2026 HM Revenue & Customs
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

import models.filing.{CreateResidencyRequest, DeleteResidencyRequest, UpdateResidencyRequest}
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import service.filing.ResidencyReturnsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ResidencyReturnsController @Inject()(
                                            cc: ControllerComponents,
                                            service: ResidencyReturnsService,
                                            auth: IdentifierAction
                                          )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def createResidency(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[CreateResidencyRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .createResidency(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[createResidency] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def updateResidency(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[UpdateResidencyRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .updateResidency(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[updateResidency] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def deleteResidency(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[DeleteResidencyRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .deleteResidency(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[deleteResidency] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }
}