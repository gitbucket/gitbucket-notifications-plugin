package gitbucket.notifications.service

import gitbucket.core.controller.Context
import gitbucket.core.model.{Account, Issue}
import gitbucket.core.service._
import RepositoryService.RepositoryInfo
import gitbucket.core
import gitbucket.core.model
import gitbucket.core.util.{LDAPUtil, Mailer}
import gitbucket.core.view.Markdown
import gitbucket.notifications.model.Profile._
import org.slf4j.LoggerFactory
import profile.blockingApi._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


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
  with IssuesService
  with LabelsService
  with PrioritiesService
  with MilestonesService
  with SystemSettingsService {

  private val logger = LoggerFactory.getLogger(classOf[IssueHook])

  override def created(issue: Issue, r: RepositoryInfo)(implicit session: Session, context: Context): Unit = {
    val markdown =
      s"""|${issue.content getOrElse ""}
          |
          |----
          |[View it on GitBucket](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"})
          |""".stripMargin

    sendAsync(issue, r, subject(issue, r), markdown)
  }

  override def addedComment(commentId: Int, content: String, issue: Issue, r: RepositoryInfo)
                           (implicit session: Session, context: Context): Unit = {
    val markdown =
      s"""|${content}
          |
          |----
          |[View it on GitBucket](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}#comment-$commentId"})
          |""".stripMargin

    sendAsync(issue, r, subject(issue, r), markdown)
  }

  override def closed(issue: Issue, r: RepositoryInfo)(implicit session: Session, context: Context): Unit = {
    val markdown =
      s"""|close #[${issue.issueId}](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"})
          |""".stripMargin

    sendAsync(issue, r, subject(issue, r), markdown)
  }

  override def reopened(issue: Issue, r: RepositoryInfo)(implicit session: Session, context: Context): Unit = {
    val markdown =
      s"""|reopen #[${issue.issueId}](${s"${context.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"})
          |""".stripMargin

    sendAsync(issue, r, subject(issue, r), markdown)
  }

  override def assigned(issue: Issue, r: RepositoryInfo, assigner: Option[String], assigned: Option[String], oldAssigned: Option[String])(implicit session: model.Profile.profile.api.Session, context: Context): Unit = {
    val assignerMessage = assigner.flatMap(getAccountByUserName(_)).map(a => s"${a.fullName}(@${a.userName})").getOrElse("unknown user")
    val assignedMessage = assigned.flatMap(getAccountByUserName(_)).map(a => s"${a.fullName}(@${a.userName})").getOrElse("not assigned")
    val oldAssignedMessage = oldAssigned.flatMap(getAccountByUserName(_, true)).map(a => s"${a.fullName}(@${a.userName})").getOrElse("not assigned")
    val markdown =
      s"""assigned from ${oldAssignedMessage} to ${assignedMessage} by ${assignerMessage}
         |""".stripMargin
    sendAsync(issue, r, subject(issue, r), markdown)
  }

  override def closedByCommitComment(issue: Issue, r: RepositoryInfo, commitMessage: String, pusher: Account)(implicit session: core.model.Profile.profile.api.Session): Unit = {
    val settings = loadSystemSettings()
    val message = s"""|close #[${issue.issueId}](${s"${settings.baseUrl}/${r.owner}/${r.name}/issues/${issue.issueId}"})
        |
        |${commitMessage}""".stripMargin
    println(message)
    sendAsyncTextOnly(issue, r, subject(issue, r), message, pusher, settings)
  }

  protected def subject(issue: Issue, r: RepositoryInfo): String = {
    s"[${r.owner}/${r.name}] ${issue.title} (#${issue.issueId})"
  }

  protected def toHtml(markdown: String, r: RepositoryInfo)(implicit context: Context): String =
    Markdown.toHtml(
      markdown         = markdown,
      repository       = r,
      branch           = r.repository.defaultBranch,
      enableWikiLink   = false,
      enableRefsLink   = true,
      enableAnchor     = false,
      enableLineBreaks = false
    )

  protected def sendAsyncTextOnly(issue: Issue, repository: RepositoryInfo, subject: String, message: String, senderAccount: Account, settings: SystemSettingsService.SystemSettings)(implicit session:Session): Unit = {
    val recipients = getRecipients(issue, senderAccount)
    val mailer = new Mailer(settings)
    val f = Future {
      recipients.foreach { address =>
        mailer.send(address, subject, message, None, Some(senderAccount))
      }
      "Notifications Successful."
    }
    f.onComplete {
      case Success(s) => logger.debug(s)
      case Failure(t) => logger.error("Notifications Failed.", t)
    }
  }

  protected def sendAsync(issue: Issue, repository: RepositoryInfo, subject: String, markdown: String)
                         (implicit session: Session, context: Context): Unit = {
    val recipients = getRecipients(issue, context.loginAccount.get)
    val mailer = new Mailer(context.settings)
    val html = toHtml(markdown, repository)
    val f = Future {
      recipients.foreach { address =>
        mailer.send(address, subject, markdown, Some(html), context.loginAccount)
      }
      "Notifications Successful."
    }
    f.onComplete {
      case Success(s) => logger.debug(s)
      case Failure(t) => logger.error("Notifications Failed.", t)
    }
  }

  protected def getRecipients(issue: Issue, loginAccount: Account)(implicit session: Session): Seq[String] = {
    getNotificationUsers(issue)
      .withFilter ( _ != loginAccount.userName )  // the operation in person is excluded
      .flatMap (
      getAccountByUserName(_)
        .filterNot (_.isGroupAccount)
        .filterNot (LDAPUtil.isDummyMailAddress)
        .filterNot (isDisableEmailNotification)
        .map (account =>
          account.mailAddress :: getAccountExtraMailAddresses(account.userName)
        )
      )
      .flatten
      .distinct
  }

}

class PullRequestHook extends IssueHook with gitbucket.core.plugin.PullRequestHook {

  override def created(issue: Issue, r: RepositoryInfo)(implicit session: Session, context: Context): Unit = {
    val markdown =
      s"""|${issue.content getOrElse ""}
          |
          |----
          |View, comment on, or merge it at:
          |${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}
          |""".stripMargin

    sendAsync(issue, r, subject(issue, r), markdown)
  }

  override def addedComment(commentId: Int, content: String, issue: Issue, r: RepositoryInfo)
                           (implicit session: Session, context: Context): Unit = {
    val markdown =
      s"""|$content
          |
          |----
          |[View it on GitBucket](${s"${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}#comment-$commentId"})
          |""".stripMargin

    sendAsync(issue, r, subject(issue, r), markdown)
  }

  override def merged(issue: Issue, r: RepositoryInfo)(implicit session: Session, context: Context): Unit = {
    val markdown =
      s"""|merge #[${issue.issueId}](${s"${context.baseUrl}/${r.owner}/${r.name}/pull/${issue.issueId}"})
          |""".stripMargin

    sendAsync(issue, r, subject(issue, r), markdown)
  }

}
