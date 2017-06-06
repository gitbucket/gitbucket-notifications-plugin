package gitbucket.notifications.view

import gitbucket.core.service.{AccountService, IssuesService, RepositoryService}
import gitbucket.notifications.service.NotificationsService


object helpers extends NotificationsService with RepositoryService with AccountService with IssuesService
