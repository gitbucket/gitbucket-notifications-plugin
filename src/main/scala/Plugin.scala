import gitbucket.core.controller.Context
import gitbucket.core.model.Issue
import gitbucket.core.plugin.Link
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.notifications.service._
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {

  override val pluginId = "notifications"

  override val pluginName = "Notifications Plugin"

  override val description = "Provides Notifications feature on GitBucket."

  override val versions = List(
    new Version("1.0.0",
      new LiquibaseMigration("update/gitbucket-notifications_1.0.xml")
    )
  )

  override val accountHooks     = Seq(new AccountHook)
  override val repositoryHooks  = Seq(new RepositoryHook)
  override val issueHooks       = Seq(new IssueHook)
  override val pullRequestHooks = Seq(new PullRequestHook)

  override val repositoryMenus = Seq(
    (repository: RepositoryInfo, context: Context) =>
      Some(Link(
        id    = "watch",
        label = "Watch",
        path  = "/watch",
        icon  = Some("menu-icon octicon octicon-eye")
      ))
  )

  override val issueSidebars = Seq(
    (issue: Issue, repository: RepositoryInfo, context: Context) =>
      context.loginAccount map { account =>
        // TODO DB access
        gitbucket.notifications.html.issue(false, issue, repository)
      }
  )

}
