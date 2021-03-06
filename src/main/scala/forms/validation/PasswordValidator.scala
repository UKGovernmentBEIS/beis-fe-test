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

package forms.validation

import cats.data.ValidatedNel
import cats.syntax.validated._
import forms.validation.FieldValidator.Normalised
import play.api.data.validation.{Constraints, Invalid, Valid}
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

case class PasswordValidator(label: Option[String] = None) extends FieldValidator[Option[String], String] {

  import EmailValidator._

  override def normalise(os: Option[String]): Option[String] = os.map(_.trim())

  def validateLength(path: String, s: Normalised[Option[String]]): ValidatedNel[FieldError, String] = {

    s match {
      case n if n.getOrElse("").length < 8 =>
        FieldError(path, Messages("error.BF033", s"'${label.getOrElse("Field")}'")).invalidNel
      case n => n.getOrElse("").validNel
    }
  }

  def validateForLettersAndNumber(path: String, s:String): ValidatedNel[FieldError, String] = {

    val regCheckNum = "^[a-zA-Z0-9]*$"    //for some reason check for Numbers is not working
    val regCheckLetters = ".*[a-zA-Z]+.*" // checks at least one char in the string
    s match {
      case n if s.forall(_.isLetter) && s.forall(_.isDigit)  =>
        n.validNel
      case n if !s.forall(_.isLetter) && !s.forall(_.isDigit)  =>
        n.validNel
      case n =>
        FieldError(path, Messages("error.BF034", s"'${label.getOrElse("Field")}'")).invalidNel
    }
  }

  //Todo:- Not used - delete
  def validateForNumber(path: String, s:String): ValidatedNel[FieldError, String] = {

    val reg = "%[a-zA-Z]%"
    s match {
      case n if !s.matches(reg) =>
        FieldError(path, Messages("error.BF034", s"'${label.getOrElse("Field")}'")).invalidNel
      case n => n.validNel
    }
  }

  override def doValidation(path: String, s: Normalised[Option[String]]): ValidatedNel[FieldError, String] = {
    implicit val messages = Messages

    validateLength(path, s).andThen(validateForLettersAndNumber(path, _))
  }

}


