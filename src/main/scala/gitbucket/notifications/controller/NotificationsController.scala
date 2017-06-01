package gitbucket.notifications.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.Implicits._
import gitbucket.core.util.ReadableUsersAuthenticator
import gitbucket.core.util.SyntaxSugars._
import gitbucket.notifications.html
import gitbucket.notifications.model.Watch
import gitbucket.notifications.service.NotificationsService
import org.scalatra.Ok

class NotificationsController extends NotificationsControllerBase
  with NotificationsService with RepositoryService with AccountService with ReadableUsersAuthenticator

trait NotificationsControllerBase extends ControllerBase {
  self: NotificationsService with RepositoryService with AccountService with ReadableUsersAuthenticator =>

  get("/:owner/:repository/watch")(readableUsersOnly { repository =>
    defining(repository.owner, repository.name) { case (owner, name) =>
      html.watch(
        // TODO default value
        getWatch(owner, name, context.loginAccount.get.userName),
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
    params.getAs[Boolean]("subscribed").map { subscribed =>
      updateIssueNotification(repository.owner, repository.name, params("id").toInt, context.loginAccount.get.userName, subscribed)
      Ok()
    } getOrElse NotFound()
  })

}
