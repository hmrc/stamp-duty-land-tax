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

import models.filing.{
  CreateVendorRequest,
  DeleteVendorRequest,
  UpdateVendorRequest
}
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import service.filing.VendorReturnsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class VendorReturnsController @Inject() (
    cc: ControllerComponents,
    service: VendorReturnsService,
    auth: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createVendor(): Action[JsValue] = auth.async(parse.json) {
    implicit request =>
      request.body
        .validate[CreateVendorRequest]
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
              .createVendor(body)
              .map { result =>
                Created(Json.toJson(result))
              }
              .recover { case t =>
                logger.error("[createVendor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
  }

  def updateVendor(): Action[JsValue] = auth.async(parse.json) {
    implicit request =>
      request.body
        .validate[UpdateVendorRequest]
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
              .updateVendor(body)
              .map { result =>
                Created(Json.toJson(result))
              }
              .recover { case t =>
                logger.error("[updateVendor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
  }

  def deleteVendor(): Action[JsValue] = auth.async(parse.json) {
    implicit request =>
      request.body
        .validate[DeleteVendorRequest]
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
              .deleteVendor(body)
              .map { result =>
                Created(Json.toJson(result))
              }
              .recover { case t =>
                logger.error("[deleteVendor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
  }

}
