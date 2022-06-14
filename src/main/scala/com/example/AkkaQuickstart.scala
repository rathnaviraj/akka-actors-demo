//#full-example
package com.example


import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.GreeterMain.SayHello

//#person-actor
object Person {

  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String, from: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
    
    context.log.info("Hello {}!", message.whom)
    
    //#person-send-messages
    message.replyTo ! Greeted(message.whom, context.self)

    Behaviors.same
  }
}

//#greeter-bot
object GreeterBot {

  def apply(max: Int): Behavior[Person.Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[Person.Greeted] = Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      context.log.info("Greeting {} to {} done", n, message.whom)
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! Person.Greet(message.whom, context.self)
        bot(n, max)
      }
    }
}
//#greeter-bot

//#greeter-main
object GreeterMain {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      //#create person actor
      val person = context.spawn(Person(), "person")

      Behaviors.receiveMessage { message =>
        //#create greet bot actor
        val bot = context.spawn(GreeterBot(max = 5), "greet-bot")

        person ! Person.Greet(message.name, bot)
        
        Behaviors.same
      }
    }
}


//#main-class
object AkkaQuickstart extends App {
  //# init actor-system
  val actorSystem: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "system")


  //#main-send-messages
  actorSystem ! SayHello("Charles")
}

