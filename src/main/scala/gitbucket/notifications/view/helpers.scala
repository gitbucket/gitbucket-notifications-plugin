package gitbucket.notifications.view

import gitbucket.core.service._
import gitbucket.notifications.service.NotificationsService


object helpers extends NotificationsService with RepositoryService with AccountService
  with IssuesService with LabelsService with PrioritiesService with MilestonesService
