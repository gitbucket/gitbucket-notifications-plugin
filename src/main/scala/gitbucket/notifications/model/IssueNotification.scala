package gitbucket.notifications.model

trait IssueNotificationComponent { self: gitbucket.core.model.Profile =>
  import profile.api._

  lazy val IssueNotifications = TableQuery[IssueNotifications]

  class IssueNotifications(tag: Tag) extends Table[IssueNotification](tag, "ISSUE_NOTIFICATION") {
    val userName             = column[String]("USER_NAME")
    val repositoryName       = column[String]("REPOSITORY_NAME")
    val issueId              = column[Int]("ISSUE_ID")
    val notificationUserName = column[String]("NOTIFICATION_USER_NAME")
    val subscribed           = column[Boolean]("SUBSCRIBED")
    def * = (userName, repositoryName, issueId, notificationUserName, subscribed).<>(IssueNotification.tupled, IssueNotification.unapply)
  }
}

case class IssueNotification(
  userName: String,
  repositoryName: String,
  issueId: Int,
  notificationUserName: String,
  subscribed: Boolean
)
