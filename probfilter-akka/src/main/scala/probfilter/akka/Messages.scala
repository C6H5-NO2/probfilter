package probfilter.akka

import akka.actor.typed.ActorRef
import akka.cluster.ddata.Replicator


object Messages {
  /** @note package-private */
  sealed trait Message

  sealed trait Request extends Message
  final case class Contains(elem: Any, replyTo: ActorRef[Response]) extends Request
  final case class Add(elem: Any, replyTo: ActorRef[AddResponse]) extends Request
  final case class Remove(elem: Any, replyTo: ActorRef[RemoveResponse]) extends Request

  /** @note package-private */
  sealed trait InternalResponse extends Message
  final case class InternalContainsResponse(rsp: Replicator.GetResponse[ReplicatedFilter], req: Contains)
    extends InternalResponse
  final case class InternalAddResponse(rsp: Replicator.UpdateResponse[ReplicatedFilter], req: Add)
    extends InternalResponse
  final case class InternalRemoveResponse(rsp: Replicator.UpdateResponse[ReplicatedFilter], req: Remove)
    extends InternalResponse

  sealed trait Response
  final case class ContainsResponse(elem: Any, flag: Boolean) extends Response
  final case class AddResponse(elem: Any, error: Option[Throwable]) extends Response
  final case class RemoveResponse(elem: Any) extends Response
}
