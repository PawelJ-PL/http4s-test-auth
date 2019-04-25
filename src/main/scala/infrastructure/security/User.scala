package infrastructure.security

case class User(id: String, origin: String, email: String, firstName: Option[String], lastName: Option[String])
