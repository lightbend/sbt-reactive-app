package controllers

import javax.inject._

import akka.actor.ActorSystem
import com.lightbend.rp.servicediscovery.scaladsl.ServiceLocator
import play.api._
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class HomeController @Inject() (cc: ControllerComponents, ws: WSClient)(implicit system: ActorSystem) extends AbstractController(cc) {
  import system.dispatcher

  val log = Logger(this.getClass)

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("Hello")
  }
}
