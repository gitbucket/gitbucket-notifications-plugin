package gitbucket.notifications.service

import gitbucket.core.controller.Context
import gitbucket.core.model.{Account, Issue}
import gitbucket.core.service._, RepositoryService.RepositoryInfo
import gitbucket.core.util.{LDAPUtil, Notifier}
import gitbucket.core.view.Markdown
import gitbucket.notifications.model.Profile._
import profile.blockingApi._


class AccountHook extends gitbucket.core.plugin.AccountHook {

  override def deleted(userName: String)(implicit session: Session): Unit = {
    IssueNotifications.filter(_.notificationUserName === userName.bind).delete
    Watches.filter(_.notificationUserName === userName.bind).delete
  }

}

class RepositoryHook extends gitbucket.core.plugin.RepositoryHook {

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

class IssueHook extends gitbucket.core.plugin.IssueHook
  with NotificationsService
  with RepositoryService
  with AccountService
  with IssuesService {

  override def created(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val markdown =
      s"""|${issue.content getOrElse ""}
          |
          |----
          |[View it on GitBucket](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"})
          |""".stripMargin

    Notifier().toNotify(subject(issue, r), markdown, Some(toHtml(markdown, r)))(recipients(issue))
  }

  override def addedComment(commentId: Int, content: String, issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val markdown =
      s"""|${content}
          |
          |----
          |[View it on GitBucket](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}#comment-$commentId"})
          |""".stripMargin

    Notifier().toNotify(subject(issue, r), markdown, Some(toHtml(markdown, r)))(recipients(issue))
  }

  override def closed(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val markdown =
      s"""|close #[${issue.issueId}](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"})
          |""".stripMargin

    Notifier().toNotify(subject(issue, r), markdown, Some(toHtml(markdown, r)))(recipients(issue))
  }

  override def reopened(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val markdown =
      s"""|reopen #[${issue.issueId}](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"})
          |""".stripMargin

    Notifier().toNotify(subject(issue, r), markdown, Some(toHtml(markdown, r)))(recipients(issue))
  }


  protected def subject(issue: Issue, r: RepositoryInfo): String = s"[${r.owner}/${r.name}] ${issue.title} (#${issue.issueId})"

  protected def toHtml(markdown: String, r: RepositoryInfo)(implicit context: Context): String =
    Markdown.toHtml(
      markdown         = markdown,
      repository       = r,
      enableWikiLink   = false,
      enableRefsLink   = true,
      enableAnchor     = false,
      enableLineBreaks = false
    )

  protected val recipients: Issue => Account => Session => Seq[String] = {
    issue => loginAccount => implicit session =>
      getNotificationUsers(issue)
        .withFilter ( _ != loginAccount.userName )  // the operation in person is excluded
        .flatMap (
          getAccountByUserName(_)
            .filterNot (_.isGroupAccount)
            .filterNot (LDAPUtil.isDummyMailAddress)
            .map (_.mailAddress)
        )
  }

}

class PullRequestHook extends IssueHook with gitbucket.core.plugin.PullRequestHook {

  override def created(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val markdown =
      s"""|${issue.content getOrElse ""}
          |
          |----
          |View, comment on, or merge it at:
          |${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}
          |""".stripMargin

    Notifier().toNotify(subject(issue, r), markdown, Some(toHtml(markdown, r)))(recipients(issue))
  }

  override def addedComment(commentId: Int, content: String, issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val markdown =
      s"""|$content
          |
          |----
          |[View it on GitBucket](${s"${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}#comment-$commentId"})
          |""".stripMargin

    Notifier().toNotify(subject(issue, r), markdown, Some(toHtml(markdown, r)))(recipients(issue))
  }

  override def merged(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val markdown =
      s"""|merge #[${issue.issueId}](${s"${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}"})
          |""".stripMargin

    Notifier().toNotify(subject(issue, r), markdown, Some(toHtml(markdown, r)))(recipients(issue))
  }

}
