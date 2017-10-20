package gitbucket.notifications.model

import gitbucket.core.model._

object Profile extends CoreProfile
  with IssueNotificationComponent
  with WatchComponent
  with NotificationsAccountComponent
