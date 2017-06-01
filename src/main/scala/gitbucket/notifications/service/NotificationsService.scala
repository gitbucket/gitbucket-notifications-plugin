package gitbucket.notifications.service

import gitbucket.notifications.model._, Profile._
import profile.blockingApi._

trait NotificationsService {

  def getWatch(owner: String, repository: String, userName: String)(implicit s: Session): Option[Watch] = {
    Watches
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind && t.notificationUserName === userName.bind)
      .firstOption
  }

  def updateWatch(owner: String, repository: String, userName: String, notification: Watch.Notification)(implicit s: Session): Unit = {
    Watches
      .filter(t => t.userName === owner.bind && t.repositoryName === repository.bind && t.notificationUserName === userName.bind)
      .delete
    Watches insert Watch(
      userName             = owner,
      repositoryName       = repository,
      notificationUserName = userName,
      notification         = notification
    )
  }

  def getIssueNotification(owner: String, repository: String, issueId: Int, userName: String)(implicit s: Session): Option[IssueNotification] = {
    IssueNotifications
      .filter { t =>
        t.userName === owner.bind && t.repositoryName === repository.bind &&
          t.issueId === issueId.bind && t.notificationUserName === userName.bind
      }
      .firstOption
  }

  def updateIssueNotification(owner: String, repository: String, issueId: Int, userName: String, subscribed: Boolean)(implicit s: Session): Unit = {
    IssueNotifications
      .filter { t =>
        t.userName === owner.bind && t.repositoryName === repository.bind &&
          t.issueId === issueId.bind && t.notificationUserName === userName.bind
      }
      .delete
    IssueNotifications insert IssueNotification(
      userName             = owner,
      repositoryName       = repository,
      issueId              = issueId,
      notificationUserName = userName,
      subscribed           = subscribed
    )
  }

}
