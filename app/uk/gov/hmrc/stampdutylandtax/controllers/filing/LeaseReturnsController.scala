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

import models.filing._
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import service.filing.LeaseReturnsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class LeaseReturnsController @Inject()(
                                        cc: ControllerComponents,
                                        service: LeaseReturnsService,
                                        auth: IdentifierAction
                                     )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def createLease(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[CreateLeaseRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .createLease(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[createLease] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def updateLease(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[UpdateLeaseRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .updateLease(body)
            .map { result =>
              Ok(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[updateLease] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def deleteLease(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[DeleteLeaseRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .deleteLease(body)
            .map { result =>
              Ok(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[deleteLease] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

}