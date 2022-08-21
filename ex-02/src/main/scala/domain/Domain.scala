package domain

import akka.actor.typed.ActorRef

trait Domain:
    //Model
    type Zone
    type City <: Zone
    type Pluviometer
    type Point2D

    //Actors
    type CityDirector
    type ZoneDirector
    type PluviometerActor
    type FirestationActor