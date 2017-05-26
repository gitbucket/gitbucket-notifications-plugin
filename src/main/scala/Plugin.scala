import gitbucket.core.plugin._
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


}
