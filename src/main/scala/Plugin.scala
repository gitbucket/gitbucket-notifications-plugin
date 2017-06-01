import gitbucket.core.controller.Context
import gitbucket.core.model.Issue
import gitbucket.core.plugin._
import gitbucket.core.service.RepositoryService.RepositoryInfo
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {

  override val pluginId: String = "notifications"

  override val pluginName: String = "Notifications Plugin"

  override val description: String = "Provides Notifications feature on GitBucket."

  override val versions: List[Version] = List(
    new Version("1.0.0",
      new LiquibaseMigration("update/gitbucket-notifications_1.0.xml")
    )
  )


  override val accountHooks: Seq[AccountHook] = Seq(
  )

  override val repositoryHooks: Seq[RepositoryHook] = Seq(
  )

  override val issueHooks: Seq[IssueHook] = Seq(
  )

  override val pullRequestHooks: Seq[PullRequestHook] = Seq(
  )

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
