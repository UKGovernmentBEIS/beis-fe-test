/*
 * Copyright (C) 2016  Department for Business, Energy and Industrial Strategy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package actions

import javax.inject.Inject

import models.{AppSectionNumber, ApplicationId, ApplicationSectionDetail}
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc._
import services.ApplicationOps

import scala.concurrent.{ExecutionContext, Future}

import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

case class AppSectionRequest[A](appSection: ApplicationSectionDetail, request: Request[A]) extends WrappedRequest[A](request)

class AppSectionAction @Inject()(applications: ApplicationOps)(implicit ec: ExecutionContext) {

  implicit val messages = Messages

  def apply(id: ApplicationId, sectionNum: AppSectionNumber): ActionBuilder[AppSectionRequest] =

    new ActionBuilder[AppSectionRequest] {

      override def invokeBlock[A](request: Request[A], next: (AppSectionRequest[A]) => Future[Result]): Future[Result] = {
        applications.sectionDetail(id, sectionNum).flatMap {
          case Some(app) =>
            next(AppSectionRequest(app, request))

          case None =>
            Future.successful (Ok(views.html.loginForm(Messages("error.BF040")) ).withNewSession)
        }
      }
    }
}

