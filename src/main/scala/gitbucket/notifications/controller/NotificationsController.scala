package gitbucket.notifications.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, IssuesService, RepositoryService}
import gitbucket.core.util.Implicits._
import gitbucket.core.util.ReadableUsersAuthenticator
import gitbucket.core.util.SyntaxSugars._
import gitbucket.notifications.html
import gitbucket.notifications.model.Watch
import gitbucket.notifications.service.NotificationsService
import org.scalatra.Ok

class NotificationsController extends NotificationsControllerBase
  with NotificationsService with RepositoryService with AccountService with IssuesService with ReadableUsersAuthenticator

trait NotificationsControllerBase extends ControllerBase {
  self: NotificationsService with RepositoryService with AccountService with IssuesService with ReadableUsersAuthenticator =>

  get("/:owner/:repository/watch")(readableUsersOnly { repository =>
    defining(repository.owner, repository.name, context.loginAccount.get.userName) { case (owner, name, userName) =>
      html.watch(
        getWatch(owner, name, userName).map(_.notification) getOrElse {
          if (autoSubscribeUsersForRepository(owner, name) contains userName) Watch.Watching else Watch.NotWatching
        },
        repository
      )
    }
  })

  ajaxPost("/:owner/:repository/watch")(readableUsersOnly { repository =>
    params.get("notification").flatMap(Watch.Notification.valueOf).map { notification =>
      updateWatch(repository.owner, repository.name, context.loginAccount.get.userName, notification)
      Ok()
    } getOrElse NotFound()
  })

  ajaxPost("/:owner/:repository/issues/:id/notification")(readableUsersOnly { repository =>
    defining(repository.owner, repository.name) { case (owner, name) =>
      getIssue(owner, name, params("id")).flatMap { issue =>
        params.getAs[Boolean]("subscribed").map { subscribed =>
          updateIssueNotification(owner, name, issue.issueId, context.loginAccount.get.userName, subscribed)
          Ok()
        }
      } getOrElse NotFound()
    }
  })

}
