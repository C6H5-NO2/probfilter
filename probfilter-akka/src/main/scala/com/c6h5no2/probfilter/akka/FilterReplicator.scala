package com.c6h5no2.probfilter.akka

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}


/**
 * Factory for the [[akka.actor.typed.Behavior Behavior]] that interacts with
 * [[ReplicatedFilter]] using [[Messages.Message]].
 */
object FilterReplicator {
  def apply(key: ReplicatedFilterKey, initial: ReplicatedFilter): Behavior[Messages.Message] = {
    Behaviors.setup[Messages.Message] { context =>
      // val node = DistributedData.apply(context.system).selfUniqueAddress
      DistributedData.withReplicatorMessageAdapter[Messages.Message, ReplicatedFilter] { adapter =>
        Behaviors.receiveMessage[Messages.Message] {
          case req: Messages.Contains =>
            adapter.askGet(
              re => Replicator.Get(key, Replicator.ReadLocal, re),
              rsp => Messages.InternalContainsResponse(rsp, req)
            )
            Behaviors.same

          case req: Messages.Add =>
            adapter.askUpdate(
              re => Replicator.Update(key, Replicator.WriteLocal, re) {
                filter => filter.getOrElse(initial).add(req.elem)
              },
              rsp => Messages.InternalAddResponse(rsp, req)
            )
            Behaviors.same

          case req: Messages.Remove =>
            adapter.askUpdate(
              re => Replicator.Update(key, Replicator.WriteLocal, re) {
                filter => filter.fold(initial)(_.remove(req.elem))
              },
              rsp => Messages.InternalRemoveResponse(rsp, req)
            )
            Behaviors.same

          case Messages.InternalContainsResponse(rsp@Replicator.GetSuccess(`key`), req) =>
            val result = rsp.get(key).contains(req.elem)
            req.replyTo tell Messages.ContainsResponse(req.elem, result)
            Behaviors.same

          case Messages.InternalContainsResponse(Replicator.NotFound(`key`), req) =>
            req.replyTo tell Messages.ContainsResponse(req.elem, false)
            Behaviors.same

          case Messages.InternalAddResponse(Replicator.UpdateSuccess(`key`), req) =>
            req.replyTo tell Messages.AddResponse(req.elem, None)
            Behaviors.same

          case Messages.InternalAddResponse(rsp@Replicator.ModifyFailure(`key`, _, _), req) =>
            req.replyTo tell Messages.AddResponse(req.elem, Some(rsp.cause))
            Behaviors.same

          case Messages.InternalRemoveResponse(Replicator.UpdateSuccess(`key`), req) =>
            req.replyTo tell Messages.RemoveResponse(req.elem)
            Behaviors.same

          case _ => Behaviors.unhandled
        }
      }
    }
  }
}
