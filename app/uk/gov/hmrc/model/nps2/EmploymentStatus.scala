/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.model.nps2

sealed trait EmploymentStatus {
  def code: Int
}

object EmploymentStatus {

  object Live extends EmploymentStatus {
    val code = 1
  }

  object Ceased extends EmploymentStatus {
    val code = 2
  }

  object PotentiallyCeased extends EmploymentStatus {
    val code = 3
  }

  case class Unknown(code: Int) extends EmploymentStatus

  val set: Set[EmploymentStatus] = Set(Live, Ceased, PotentiallyCeased)

  def apply(i: Int): EmploymentStatus = set.find {
    _.code == i
  }.
    getOrElse {
    Unknown(i)
  }
}
