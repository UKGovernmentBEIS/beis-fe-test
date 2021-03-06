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

package controllers

import java.util
import java.util.Base64
import javax.inject.Inject

import controllers.FieldCheckHelpers.FieldErrors
import controllers.JsonHelpers.formToJson
import forms.validation._
import models.UserId
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.activiti.engine.repository.ProcessDefinition
import org.activiti.engine.task.Task
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json
import org.activiti.engine.{ProcessEngine, ProcessEngines}

import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, Future}
import org.apache.commons.lang3.StringUtils
import services.RestService.JsonParseException
import services.UserOps

//import play.api.data.validation.Validation
import play.api.data.validation._

import cats.data.ValidatedNel
import cats.syntax.cartesian._
import cats.syntax.validated._
import forms.validation.FieldValidator.Normalised
import scala.util.{Failure, Success, Try}

import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

/********************************************************************************
  This file is for temporary Login till any Security component is deployed.
  This file also for Activity samples.
  Please donot use this login file. i.e dont use http://localhost:9001/login
  Use only http://localhost:9001
 *********************************************************************************/

trait SessionUser{

  implicit def sessionUser(implicit session: Session): String = {
    //val usr =  for (suser<- session.get("username")) yield suser
    session.get("username").getOrElse(Messages("error.BF005"))
  }
}

class UserController @Inject()(users: UserOps)(implicit ec: ExecutionContext)
  extends Controller with ApplicationResults /*with Constraint[String]*/ {

  implicit val userIdWrites = Json.writes[UserId]
  implicit val loginFormWrites = Json.writes[Login]
  implicit val loginWrites = Json.writes[LoginForm]
  implicit val regnWrites = Json.writes[Registration]
  implicit val regnFormWrites = Json.writes[RegistrationForm]
  implicit val resetPasswordWrites = Json.writes[ResetPassword]
  implicit val messages = Messages

  val loginform:Form[LoginForm] = Form(
    mapping(
      "name" -> text,
      "password" -> text
    ) (LoginForm.apply)(LoginForm.unapply) verifying ("Invalid email or password", result => result match {
      case loginForm => checkNotNull(loginForm.name, loginForm.password)
    })
  )

  val registrationform:Form[RegistrationForm] = Form(
    mapping(
      "name" -> text,
      "password" -> text,
      "confirmpassword" -> text,
      "email" -> text
    ) (RegistrationForm.apply)(RegistrationForm.unapply) verifying ("Invalid email or password", result => result match {
      case registrationForm => checkNotNull(registrationForm.name, registrationForm.password)
    })
  )

  val forgotpasswordform:Form[ForgotPasswordForm] = Form(
    mapping(
      "email" -> text
    ) (ForgotPasswordForm.apply)(ForgotPasswordForm.unapply) verifying ("Invalid email", result => result match {
      case forgotpasswordform => checkEmailNotNull(forgotpasswordform.email.replaceAll("\\s+", ""))
    })
  )


  val resetpasswordform:Form[ResetPasswordForm] = Form(
    mapping(
      "refno" -> text,
      "password" -> text,
      "confirmpassword" -> text
    ) (ResetPasswordForm.apply)(ResetPasswordForm.unapply) verifying ("Reset Password", result => result match {
      case resetpasswordform => checkEmailNotNull(resetpasswordform.password replaceAll("\\s+", ""))
    })
  )

  def checkNotNull(username: String, password: String) =
    (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password))

  def checkEmailNotNull(email: String) =
    (!StringUtils.isEmpty(email))

  def checkPassword(password: String) = {
      (password.length > 8 )
  }

  def loginForm = Action{ implicit request =>
    implicit val session: Session = request.session
    implicit var suser = request.session.get("username").getOrElse(Messages("error.BF005"))
    Ok(views.html.loginForm("", Option(loginform)))
  }

  def basicAuth(pswd: String) = {
    new String(Base64.getEncoder.encode((pswd).getBytes))
  }

  def nullSpaceCheck_(fld : String, value : String): List[FieldError] = {

    value.split(" ").length > 1 match {
      case true =>   List(FieldError("name", Messages("error.BF010", fld, s"'$value'")))
      case false => List()
    }
  }

  def confirmPasswordCheck(password:String,confirmpassword:String): ValidatedNel[FieldError, String] = {

    password.equals(confirmpassword) match {
      case false =>  FieldError("password", Messages("error.BF003")).invalidNel
      case true => "".validNel
    }
  }

  def refNoCheck(refNo:String): List[FieldError] = {

    Try(refNo.toLong) match {
      case Failure(e) => List(FieldError("refno", Messages("error.BF004")))
      case Success(v) => List()
    }
  }

  def mandatoryFieldCheck(path: String, s: Option[String], displayName: String): List[FieldError] =
    (MandatoryValidator(Some(displayName)).validate(path, s).andThen(NullSpaceValidator(Some(displayName)).validate(path, _)))
      .fold(_.toList, _ => List())

  def passswordCheck(path: String, s: Option[String], displayName: String): List[FieldError] =
    (PasswordValidator(Some(displayName)).validate(path, s)).fold(_.toList, _ => List())

  def registrationSubmit =  Action.async(JsonForm.parser)  { implicit request =>

    val jsObj = Json.toJson(request.body.values).as[JsObject] + ("id" -> Json.toJson(0))
    val username = (request.body.values \ "name").validate[String].getOrElse("NA")
    val password = (request.body.values \ "password").validate[String].getOrElse("NA")
    val confirmpassword = (request.body.values \ "confirmpassword").validate[String].getOrElse("NA")
    val email = (request.body.values \ "email").validate[String].getOrElse("NA")

    val regn = Registration(UserId(username), password, email)

    val mandatoryCheck = (mandatoryFieldCheck(s"name", Some(username), "name") ++
      mandatoryFieldCheck(s"email", Some(email), "email") ++
      mandatoryFieldCheck(s"password", Some(password), "password") ++
      mandatoryFieldCheck(s"confirmpassword", Some(confirmpassword), "confirmpassword"))

    val emailvalidator = EmailValidator(Option("email")).validate("email", email)

    val errors = mandatoryCheck ++
      confirmPasswordCheck(password, confirmpassword).fold(_.toList, _ => List()) ++
      passswordCheck("password", Some(password), "password") ++
      emailvalidator.fold(_.toList, _ => List())

    errors.isEmpty match {
      case true =>
        users.register(Json.toJson(regn).as[JsObject] + ("id" -> Json.toJson(0))).flatMap{

            case Some(errCode) => {
                /** TODO *************
                  * need to get the exception TYPE from backend for the exceptions and show them
                  * or need to get the error number to select the error text from any resource bundle or properties
                  */
                if(errCode.indexOf("error") != -1) {
                  val username = (request.body.values \ "name").validate[String].getOrElse("NA")
                  val errorMsg = Messages(errCode, s"'$username'")

                  Future.successful(Ok(views.html.registrationForm(registrationform, List(FieldError("name", errorMsg)))
                  (Flash(Map("name" -> username, "email" -> email))) ))
                }
                else if(errCode.indexOf("success") != -1)
                  Future.successful(Ok(views.html.loginForm(Messages("error.BF000"), Option(loginform), Some(true))))
                else
                  Future.successful(Ok(views.html.loginForm(Messages("error.BF000"), Option(loginform))))
            }
              case None =>
                Future.successful(Ok(views.html.loginForm(Messages("error.BF002"), Option(loginform))))
          }
      case false =>
        val errMsg = Messages("error.BF001")
        Future.successful(Ok(views.html.registrationForm(registrationform, errors)
                    (Flash(Map("name" -> username, "email" -> email)))  ))
    }
  }

  def forgotPasswordSubmit = Action.async(JsonForm.parser)  { implicit request =>
    val username = (request.body.values \ "name").validate[String].getOrElse("NA")
    val email = (request.body.values \ "email").validate[String].getOrElse("NA")
    users.forgotpassword(Json.toJson(request.body.values).as[JsObject]).flatMap{
      case Some(errCode) => {
         if (errCode.indexOf("error") != -1) {
          val username = (request.body.values \ "name").validate[String].getOrElse("NA")
          Future.successful(Ok(views.html.forgotPasswordForm(Messages(errCode), forgotpasswordform)
          (Flash(Map("name" -> username, "email" -> email))) ))
        }
        else
          Future.successful(Ok(views.html.forgotPasswordConfirm(email)))
      }
      case None =>
        Future.successful(Ok(views.html.forgotPasswordForm(Messages("error.BF007"), forgotpasswordform)
        (Flash(Map("name" -> username, "email" -> email))) ))
    }
  }

  def loginFormSubmit = Action.async(JsonForm.parser)  { implicit request =>

    val username = (request.body.values \ "name").validate[String].getOrElse("NA")
    val passwrd = (request.body.values \ "password").validate[String].getOrElse("NA")
    users.login(Json.toJson(Login(UserId(username), passwrd)).as[JsObject]).flatMap{
      case Some(msg) =>
        Future.successful(Redirect(routes.DashBoardController.applicantDashBoard())
          .withSession((Security.username -> username), ("sessionTime" -> System.currentTimeMillis.toString)))
      case None =>
        val errMsg = Messages("error.BF002")
        Future.successful(Ok(views.html.loginForm(errMsg, Option(loginform))))
    }
  }


  def resetPasswordSubmit = Action.async(JsonForm.parser)  { implicit request =>

    val refno = (request.body.values \ "refno").validate[String].getOrElse("NA")
    val password = (request.body.values \ "password").validate[String].getOrElse("NA")
    val confirmpassword = (request.body.values \ "confirmpassword").validate[String].getOrElse("NA")


    val mandatoryCheck = (mandatoryFieldCheck(s"password", Some(password), "password") ++
      mandatoryFieldCheck(s"confirmpassword", Some(confirmpassword), "confirmpassword"))

    val errors = mandatoryCheck ++
      confirmPasswordCheck(password, confirmpassword).fold(_.toList, _ => List()) ++
      passswordCheck("password", Some(password), "password")

    errors.isEmpty match {
      case true =>
        users.resetpassword(Json.toJson(ResetPassword(refno, password)).as[JsObject]).flatMap{
          case Some(1) =>
            Future.successful(Ok(views.html.loginForm("", Option(loginform))))
          case Some(0) =>
            val errs = List(FieldError("password", Messages("error.BF035")))
            Future.successful(Ok(views.html.resetPasswordForm(errs, refno, resetpasswordform)))
        }
      case false =>
        val errMsg = Messages("error.BF001")
        Future.successful(Ok(views.html.resetPasswordForm(errors, refno, resetpasswordform)))
    }
  }


  def logOut = Action{
    Ok(views.html.loginForm("", Option(loginform))).withNewSession
  }

  def registrationForm = Action{ implicit request =>
    //Ok(views.html.registrationForm("", registrationform))
    Ok(views.html.registrationForm(registrationform, List()))
  }

  def forgotPasswordForm = Action{implicit request =>
    Ok(views.html.forgotPasswordForm("", forgotpasswordform))
  }

  def resetPasswordForm(refno: String) = Action{
    Ok(views.html.resetPasswordForm(List(), refno, resetpasswordform))
  }

  def start(pid:Int, processEngine: ProcessEngineWrapper){
    processEngine.engine.getRuntimeService().startProcessInstanceByKey("logging-test")
  }

 }


case class LoginForm(name: String, password: String)
case class Login(name: UserId, password: String)
case class RegistrationForm(name: String, password: String, confirmpassword: String, email: String)
case class Registration(name: UserId, password : String, email: String)
//case class RegistrationFormValues(name: Option[String]= None, password: Option[String]= None, confirmpassword: Option[String]= None, email: String)
case class ForgotPasswordForm(email: String)
case class ResetPasswordForm(refno: String, password: String, confirmpassword: String)
case class ResetPassword(refno: String, password: String)
