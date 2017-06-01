package gitbucket.notifications.model

trait WatchComponent { self: gitbucket.core.model.Profile =>
  import profile.api._

  implicit val watchNotificationType = MappedColumnType.base[Watch.Notification, String](_.id, Watch.Notification.valueOf(_).get)

  lazy val Watches = TableQuery[Watches]

  class Watches(tag: Tag) extends Table[Watch](tag, "WATCH") {
    val userName             = column[String]("USER_NAME")
    val repositoryName       = column[String]("REPOSITORY_NAME")
    val notificationUserName = column[String]("NOTIFICATION_USER_NAME")
    val notification         = column[Watch.Notification]("NOTIFICATION")
    def * = (userName, repositoryName, notificationUserName, notification) <> ((Watch.apply _).tupled, Watch.unapply)
  }
}

case class Watch(
  userName: String,
  repositoryName: String,
  notificationUserName: String,
  notification: Watch.Notification
)

object Watch {
  abstract sealed class Notification(val id: String, val name: String)
  case object Watching extends Notification("watching", "Watching")
  case object NotWatching extends Notification("not_watching", "Not watching")
  case object Ignoring extends Notification("ignoring", "Ignoring")

  object Notification {
    val values: Seq[Notification] = Seq(Watching, NotWatching, Ignoring)
    def valueOf(id: String): Option[Notification] = values.find(_.id == id)
  }
}
