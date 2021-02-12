/*
 * Copyright 2021 HM Revenue & Customs
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

package exceptions

case class ServiceNotAvailableException(message: String ) extends Exception(message)
case class InternalServerErrorException(message: String ) extends Exception(message)
case object AlreadyRegisteredException extends Exception("Already registered")
case object DuplicateSubmissionException extends Exception("Duplicate submission")
case object InvalidCorrelationIdException extends Exception("Invalid correlation ID")
case object EtmpCacheDataStaleException extends Exception("Etmp data is stale")
case object NotFoundException extends Exception("Not found")
case object NoMatchException extends Exception("No matched")
case object BadRequestException extends Exception("Bad request")
case object MaxRetriesAttemptedException extends Exception("Max retries attempted")
