package com.c6h5no2.probfilter.akka

import akka.actor.typed.ActorRef
import akka.cluster.ddata.Replicator


object Messages {
  private[akka] sealed trait Message

  sealed trait Request extends Message
  final case class Contains(elem: Any, replyTo: ActorRef[ContainsResponse]) extends Request
  final case class Add(elem: Any, replyTo: ActorRef[AddResponse]) extends Request
  final case class Remove(elem: Any, replyTo: ActorRef[RemoveResponse]) extends Request

  private[akka] sealed trait InternalResponse extends Message
  private[akka] final case class InternalContainsResponse(rsp: Replicator.GetResponse[ReplicatedFilter], req: Contains) extends InternalResponse
  private[akka] final case class InternalAddResponse(rsp: Replicator.UpdateResponse[ReplicatedFilter], req: Add) extends InternalResponse
  private[akka] final case class InternalRemoveResponse(rsp: Replicator.UpdateResponse[ReplicatedFilter], req: Remove) extends InternalResponse

  sealed trait Response
  final case class ContainsResponse(elem: Any, result: Boolean) extends Response
  final case class AddResponse(elem: Any, error: Option[Throwable]) extends Response
  final case class RemoveResponse(elem: Any) extends Response
}
