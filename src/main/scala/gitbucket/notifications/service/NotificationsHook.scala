package gitbucket.notifications.service

import gitbucket.core.controller.Context
import gitbucket.core.model.{Account, Issue}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.util.Notifier
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

class IssueHook extends gitbucket.core.plugin.IssueHook {

  override def created(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    Notifier().toNotify(
      subject(issue, r),
      message(issue.content getOrElse "", r)(content => s"""
        |$content<br/>
        |--<br/>
        |<a href="${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"}">View it on GitBucket</a>
        """.stripMargin)
    )(recipients(issue))
  }

  override def addedComment(commentId: Int, content: String, issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    Notifier().toNotify(
      subject(issue, r),
      message(content, r)(content => s"""
        |$content<br/>
        |--<br/>
        |<a href="${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}#comment-$commentId"}">View it on GitBucket</a>
        """.stripMargin)
    )(recipients(issue))
  }

  override def closed(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    Notifier().toNotify(
      subject(issue, r),
      message("close", r)(content => s"""
        |$content <a href="${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"}">#${issue.issueId}</a>
        """.stripMargin)
    )(recipients(issue))
  }

  override def reopened(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    Notifier().toNotify(
      subject(issue, r),
      message("reopen", r)(content => s"""
        |$content <a href="${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"}">#${issue.issueId}</a>
        """.stripMargin)
    )(recipients(issue))
  }


  protected val subject: (Issue, RepositoryInfo) => String =
    (issue, r) => s"[${r.owner}/${r.name}] ${issue.title} (#${issue.issueId})"

  protected val message: (String, RepositoryInfo) => (String => String) => String =
    (content, r) => msg => msg(Markdown.toHtml(
      markdown         = content,
      repository       = r,
      enableWikiLink   = false,
      enableRefsLink   = true,
      enableAnchor     = false,
      enableLineBreaks = false
    ))

  // TODO
  protected val recipients: Issue => Account => Session => Seq[String] = {
    issue => loginAccount => implicit session =>
      Seq("")
  }

}

class PullRequestHook extends IssueHook with gitbucket.core.plugin.PullRequestHook {

  override def created(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    val url = s"${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}"
    Notifier().toNotify(
      subject(issue, r),
      message(issue.content getOrElse "", r)(content => s"""
        |$content<hr/>
        |View, comment on, or merge it at:<br/>
        |<a href="$url">$url</a>
        """.stripMargin)
    )(recipients(issue))
  }

  override def addedComment(commentId: Int, content: String, issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    Notifier().toNotify(
      subject(issue, r),
      message(content, r)(content => s"""
        |$content<br/>
        |--<br/>
        |<a href="${s"${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}#comment-$commentId"}">View it on GitBucket</a>
        """.stripMargin)
    )(recipients(issue))
  }

  override def merged(issue: Issue, r: RepositoryInfo)(implicit context: Context): Unit = {
    Notifier().toNotify(
      subject(issue, r),
      message("merge", r)(content => s"""
        |$content <a href="${s"${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}"}">#${issue.issueId}</a>
        """.stripMargin)
    )(recipients(issue))
  }

}
