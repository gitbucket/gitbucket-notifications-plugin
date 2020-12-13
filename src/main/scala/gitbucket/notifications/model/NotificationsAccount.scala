package gitbucket.notifications.model

trait NotificationsAccountComponent { self: gitbucket.core.model.Profile =>
  import profile.api._

  lazy val NotificationsAccounts = TableQuery[NotificationsAccounts]

  class NotificationsAccounts(tag: Tag) extends Table[NotificationsAccount](tag, "NOTIFICATIONS_ACCOUNT") {
    val userName     = column[String]("USER_NAME")
    val disableEmail = column[Boolean]("DISABLE_EMAIL")
    def * = (userName, disableEmail).<>(NotificationsAccount.tupled, NotificationsAccount.unapply)
  }
}

case class NotificationsAccount(
  userName: String,
  disableEmail: Boolean
)
