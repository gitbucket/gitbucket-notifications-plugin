package gitbucket.notifications.service

import gitbucket.core.plugin._
import gitbucket.notifications.model.Profile._
import profile.blockingApi._

trait NotificationsAccountHook extends AccountHook {

  override def deleted(userName: String)(implicit session: Session): Unit = {
    IssueNotifications.filter(_.notificationUserName === userName.bind).delete
    Watches.filter(_.notificationUserName === userName.bind).delete
  }

}
