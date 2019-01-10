package controllers

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class HelloController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def index() = Action { _ =>
    Ok("Hello, World")
  }
}