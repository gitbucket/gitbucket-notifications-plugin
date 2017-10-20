package gitbucket.notifications.service

import gitbucket.core.model.{Account, Issue}
import gitbucket.core.service.{AccountService, IssuesService, RepositoryService}
import gitbucket.notifications.model._, Profile._
import profile.blockingApi._

trait NotificationsService {
  self: RepositoryService with AccountService with IssuesService =>

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

  def isDisableEmailNotification(account: Account)(implicit s: Session): Boolean = {
    NotificationsAccounts.filter(_.userName === account.userName.bind).firstOption.exists(_.disableEmail)
  }

  def updateEmailNotification(userName: String, disable: Boolean)(implicit s: Session): Unit = {
    NotificationsAccounts.filter(_.userName === userName.bind).delete
    if (disable) NotificationsAccounts insert NotificationsAccount(userName = userName, disableEmail = true)
  }

  def autoSubscribeUsersForRepository(owner: String, repository: String)(implicit s: Session): List[String] = {
    // individual repository's owner
    owner ::
    // group members of group repository
    getGroupMembers(owner).map(_.userName) :::
    // collaborators
    getCollaboratorUserNames(owner, repository)
  }

  def getNotificationUsers(issue: Issue)(implicit s: Session): List[String] = {
    val watches = Watches.filter(t =>
      t.userName === issue.userName.bind && t.repositoryName === issue.repositoryName.bind
    ).list
    val notifications = IssueNotifications.filter(t =>
      t.userName === issue.userName.bind && t.repositoryName === issue.repositoryName.bind && t.issueId === issue.issueId.bind
    ).list

    (
      Seq(
        // auto-subscribe users for repository
        autoSubscribeUsersForRepository(issue.userName, issue.repositoryName) :::
        // watching users
        watches.withFilter(_.notification == Watch.Watching).map(_.notificationUserName),
        // participants
        issue.openedUserName ::
        getComments(issue.userName, issue.repositoryName, issue.issueId).map(_.commentedUserName),
        // subscribers
        notifications.withFilter(_.subscribed).map(_.notificationUserName)
      ) zip Seq(
        // not watching users
        watches.withFilter(_.notification == Watch.NotWatching).map(_.notificationUserName),
        // ignoring users
        watches.withFilter(_.notification == Watch.Ignoring).map(_.notificationUserName),
        // unsubscribers
        notifications.withFilter(!_.subscribed).map(_.notificationUserName)
      )
    ).foldLeft[List[String]](Nil){ case (res, (add, remove)) =>
      (add ++ res).distinct diff remove
    }

  }

}
