import gitbucket.core.controller.Context
import gitbucket.core.model.Issue
import gitbucket.core.service.{AccountService, IssuesService, RepositoryService}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.util.Implicits.request2Session
import gitbucket.notifications._
import gitbucket.notifications.model.Watch
import gitbucket.notifications.service.NotificationsService
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin
  with NotificationsService with RepositoryService with AccountService with IssuesService {

  override val pluginId = "notifications"

  override val pluginName = "Notifications Plugin"

  override val description = "Provides Notifications feature on GitBucket."

  override val versions = List(
    new Version("1.0.0",
      new LiquibaseMigration("update/gitbucket-notifications_1.0.xml")
    )
  )

  override val controllers = Seq(
    "/*" -> new controller.NotificationsController()
  )

  override val accountHooks     = Seq(new service.AccountHook)
  override val repositoryHooks  = Seq(new service.RepositoryHook)
  override val issueHooks       = Seq(new service.IssueHook)
  override val pullRequestHooks = Seq(new service.PullRequestHook)

  override val repositoryHeaders = Seq(
    (repository: RepositoryInfo, context: Context) =>  {
      context.loginAccount.map { loginAccount =>
        implicit val session = request2Session(context.request)

        html.watch(getWatch(repository.owner, repository.name, loginAccount.userName).map(_.notification) getOrElse {
          if (autoSubscribeUsersForRepository(repository.owner, repository.name) contains loginAccount.userName) Watch.Watching else Watch.NotWatching
        }, repository)(context)
      }
    }
  )

  override val issueSidebars = Seq(
    (issue: Issue, repository: RepositoryInfo, context: Context) =>
      context.loginAccount map { account =>
        implicit val session = request2Session(context.request)

        html.issue(
          view.helpers.getNotificationUsers(issue).contains(account.userName),
          issue,
          repository)(context)
      }
  )

}
