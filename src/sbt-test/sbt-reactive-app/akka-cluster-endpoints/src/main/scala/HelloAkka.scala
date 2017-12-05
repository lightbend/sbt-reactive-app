package hello.akka

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.{ Actor, ActorSystem, Props }
import com.typesafe.config.ConfigFactory

final case class Greet(name: String)

class GreeterActor extends Actor {
  val cluster = Cluster(context.system)

  override def preStart = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop = {
    cluster.unsubscribe(self)
  }

  def receive = {
    case Greet(name) =>
      println(s"Hello, $name")
    case MemberUp(member) =>
      println(s"Member up: $member")
    case MemberRemoved(member, previousStatus) =>
      println(s"Member down: $member")
    case _: MemberEvent => // ignore
  }
}

object HelloAkka {
  def main(args: Array[String]) = {
    if (args.isEmpty)
      startup(Seq("2551", "2552", "0"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]) = {
    ports foreach { port =>
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
        .withFallback(ConfigFactory.load())

      val system = ActorSystem("ClusterSystem", config)
      val actor = system.actorOf(Props[GreeterActor], name = "GreeterActor")

      actor ! Greet(port)
    }
  }
}