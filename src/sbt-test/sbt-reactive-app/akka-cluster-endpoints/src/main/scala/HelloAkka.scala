package hello.akka

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.{ Actor, ActorSystem, Props }
import akka.discovery._
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
    startup()
  }

  def startup() = {
    val system = ActorSystem("ClusterSystem")
    val discovery = ServiceDiscovery(system).discovery
    val actor = system.actorOf(Props[GreeterActor], name = "GreeterActor")

    actor ! Greet("[unnamed]")
  }
}