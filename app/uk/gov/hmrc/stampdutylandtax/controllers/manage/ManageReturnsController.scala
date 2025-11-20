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

package uk.gov.hmrc.stampdutylandtax.controllers.manage

import models.auth.IdentifierRequest
import models.manage.SdltReturnRecordRequest
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ActionBuilder, AnyContent, ControllerComponents}
import service.ManageReturnsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ManageReturnsController @Inject()(
                                         cc: ControllerComponents,
                                         service: ManageReturnsService,
                                         identify: IdentifierAction
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  private lazy val auth: ActionBuilder[IdentifierRequest, AnyContent] = identify

  def getReturns: Action[JsValue] =
    auth.async(parse.json) { implicit request =>
      request.body
        .validate[SdltReturnRecordRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          returnRecordRequest =>
            service
              .getReturns(returnRecordRequest)
              .map { result =>
                Ok(Json.toJson(result))
              }
              .recover { case t =>
                logger.error("[ManageReturnsController][getReturns] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

}