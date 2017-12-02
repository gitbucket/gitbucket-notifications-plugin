# gitbucket-notifications-plugin [![Build Status](https://travis-ci.org/gitbucket/gitbucket-notifications-plugin.svg)](https://travis-ci.org/gitbucket/gitbucket-notifications-plugin)

This plug-in provides notifications feature on GitBucket.

Plugin version | GitBucket version
:--------------|:--------------------
1.4.0          | 4.19.0
1.2.x, 1.3.x   | 4.17.x - 4.18.x
1.1.x          | 4.16.x
1.0.x          | 4.15.x

## Features

The current version of plug-in provides features such as:

- Pre-included notifications (see below)
- Watching repositories
- Subscribing to issues

### Pre-included notifications

GitBucket can send email notifications to users if this feature is enabled by an administrator.

You'll automatically receive these notifications when:

- Opened issues (new issues, new pull requests)
    - When a record is inserted into the ```ISSUE``` table
- Comments
    - Among the records in the ```ISSUE_COMMENT``` table, them to be counted as a comment (i.e. the record ```ACTION``` column value is "comment" or "close_comment" or "reopen_comment") are inserted
- Updated state (close, reopen, merge)
    - When the ```CLOSED``` column value is updated

Notified users are as follows:

- individual repository's owner
- group members of group repository
- collaborators
- participants

However, the person performing the operation is excluded from the notification.

### Watching repositories

When you watch a repository, you get notifications.
You can unwatch a repository to receive notifications when participating.
If you won't receive any notifications, you select Ignoring.

### Subscribing to issues

You can subscribe or unsubscribe to individual issues.
