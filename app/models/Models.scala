package models

case class SuggestedSnack(id: Int, lastSuggested: String, votes: Int)

case class ServiceSnack(id: Int, name: String, optional: Boolean, purchaseLocations: String, purchaseCount: Int,
                 lastPurchaseDate: Option[String])

case class SnackDetailed(id: Int, optional: Boolean, name: String, lastSuggested: String,
                         lastPurchaseDate: Option[String], votes: Int, purchaseLocations: String)
