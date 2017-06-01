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

trait NotificationsRepositoryHook extends RepositoryHook {

  override def deleted(owner: String, repository: String)(implicit session: Session): Unit =  {
    IssueNotifications.filter(t => t.userName === owner.bind && t.repositoryName === repository.bind).delete
    Watches.filter(t => t.userName === owner.bind && t.repositoryName === repository.bind).delete
  }

  override def renamed(owner: String, repository: String, newRepository: String)(implicit session: Session): Unit = {
    rename(owner, repository, owner, newRepository)
  }

  override def transferred(owner: String, newOwner: String, repository: String)(implicit session: Session): Unit = {
    rename(owner, repository, newOwner, repository)
  }

  // TODO select - insert
  private def rename(owner: String, repository: String, newOwner: String, newRepository: String)(implicit session: Session) = {
    val n = IssueNotifications.filter(t => t.userName === owner.bind && t.repositoryName === repository.bind).list
    val w = Watches.filter(t => t.userName === owner.bind && t.repositoryName === repository.bind).list

    deleted(owner, repository)

    IssueNotifications.insertAll(n.map(_.copy(userName = newOwner, repositoryName = newRepository)) :_*)
    Watches.insertAll(w.map(_.copy(userName = newOwner, repositoryName = newRepository)) :_*)
  }

}

trait NotificationsIssueHook extends IssueHook {



}
