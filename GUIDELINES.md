# Overview

This document captures development standards and architecture decisions of this project as a point of reference.

## Testing

Unsere Unittests nutzen das [Kotest-Framework](https://kotest.io) mit [JUnit](https://junit.org) im Hintergrund.
Dabei setzen wir auf die [WordSpec](https://kotest.io/styles/#word-spec), da man damit semantisch übersichtlich sowohl einfache Tests als auch Behavior Driven Development umsetzen kann.

Bisherige Tests nutzen die StringSpec, welche jedoch schnell unübersichtlich wird da sie keine Verschachtelung erlaubt, und im server-modul gibt es noch einige JUnit5-Tests.
Diese sollten bei größeren Änderungen direkt zum neuen Stil migriert werden.

## XStream

All network communication (client-server) is done via XML, in our JVM implementation the [XStream library](https://x-stream.github.io)
handles the serialization and deserialization from and to objects.

To implement the protocol properly it requires annotations.
Apart from the persistent [sdk protocol classes](sdk/src/server-api) this is particularly relevant when implementing the `Move` and `GameState` classes in the current plugin,
including all types used in their non-volatile fields (otherwise marked with @XStreamOmitField) such as `Board` and `Field`.

## Cloning

Relevant discussion: https://github.com/CAU-Kiel-Tech-Inf/backend/pull/148

To enable cloning, we implement deep copy constructors together with a clone method which defers to the copy constructors.
This is needed for all shared plugin classes that hold state and which are not immutable.
Small classes (such as Field) should be immutable, so they can be shared instead of cloning them.

It might be interesting to consider replacing cloning with implicit sharing/copy-on-write semantics to make search algorithms more efficient:
https://doc.qt.io/qt-5/implicit-sharing.html#implicit-sharing-in-detail

## ServiceLoader

We recently introduced the use of the [Java built-in DI facility](https://itnext.io/serviceloader-the-built-in-di-framework-youve-probably-never-heard-of-1fa68a911f9b) [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
to make some year-specific implementations from the plugin accessible in the sdk and server.

Currently there are two interfaces, [IGamePlugin](sdk/src/server-api/sc/api/plugins/IGamePlugin.java) and [XStreamProvider]( sdk/src/server-api/sc/networking/XStreamProvider.kt), which are implemented in the plugin and then loaded through a ServiceLoader.
The information which implementations to use resides in [resources/META-INF/services](plugin/src/resources/META-INF/services).

## Protocol Message Classes

### [sdk/server-api/sc.protocol](sdk/src/server-api/sc/protocol)

(*Request) Ask for an action or information  
(? extends [AdminLobbyRequest](sdk/src/server-api/sc/protocol/requests/ILobbyRequest.kt)) Requires authentication

#### [Responses](sdk/src/server-api/sc/protocol/responses)

If it extends `ProtocolMessage` directly, it is wrapped in a [RoomPacket](sdk/src/server-api/sc/protocol/responses/RoomPacket.kt)
and sent to a specific room, otherwise it has to extend `ILobbyRequest` and is sent to LobbyListeners.

(*Response) Response to a request  
(*Event) Update to all observers
