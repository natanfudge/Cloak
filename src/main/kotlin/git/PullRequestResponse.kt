package cloak.git

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PullRequestResponse(
    val url: String? = null,
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("diff_url")
    val diffUrl: String? = null,
    @SerialName("patch_url")
    val patchUrl: String? = null,
    @SerialName("issue_url")
    val issueUrl: String? = null,
    @SerialName("commits_url")
    val commitsUrl: String? = null,
    @SerialName("review_comments_url")
    val reviewCommentsUrl: String? = null,
    @SerialName("review_comment_url")
    val reviewCommentUrl: String? = null,
    @SerialName("comments_url")
    val commentsUrl: String? = null,
    @SerialName("statuses_url")
    val statusesUrl: String? = null,
    val number: Long? = null,
    val state: String? = null,
    val locked: Boolean? = null,
    val title: String? = null,
    val user: User? = null,
    val body: String? = null,
    val labels: List<Label>? = null,
    val milestone: Milestone? = null,
    @SerialName("active_lock_reason")
    val activeLockReason: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("closed_at")
    val closedAt: String? = null,
    @SerialName("merged_at")
    val mergedAt: String? = null,
    @SerialName("merge_commit_sha")
    val mergeCommitSha: String? = null,
    val assignee: User? = null,
    val assignees: List<User>? = null,
    @SerialName("requested_reviewers")
    val requestedReviewers: List<User>? = null,
    @SerialName("requested_teams")
    val requestedTeams: List<RequestedTeam>? = null,
    val head: Head? = null,
    val base: Base? = null,
    @SerialName("_links")
    val links: Links? = null,
    @SerialName("author_association")
    val authorAssociation: String? = null,
    val draft: Boolean? = null,
    val merged: Boolean? = null,
    val mergeable: Boolean? = null,
    val rebaseable: Boolean? = null,
    @SerialName("mergeable_state")
    val mergeableState: String? = null,
    @SerialName("merged_by")
    val mergedBy: User? = null,
    val comments: Long? = null,
    @SerialName("review_comments")
    val reviewComments: Long? = null,
    @SerialName("maintainer_can_modify")
    val maintainerCanModify: Boolean? = null,
    val commits: Long? = null,
    val additions: Long? = null,
    val deletions: Long? = null,
    @SerialName("changed_files")
    val changedFiles: Long? = null
)


@Serializable
data class Label(
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    val url: String? = null,
    val name: String? = null,
    val description: String? = null,
    val color: String? = null,
    val default: Boolean? = null
)

@Serializable
data class Milestone(
    val url: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("labels_url")
    val labelsUrl: String? = null,
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    val number: Long? = null,
    val state: String? = null,
    val title: String? = null,
    val description: String? = null,
    val creator: User? = null,
    @SerialName("open_issues")
    val openIssues: Long? = null,
    @SerialName("closed_issues")
    val closedIssues: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("closed_at")
    val closedAt: String? = null,
    @SerialName("due_on")
    val dueOn: String? = null
)


@Serializable
data class RequestedTeam(
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    val url: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val privacy: String? = null,
    val permission: String? = null,
    @SerialName("members_url")
    val membersUrl: String? = null,
    @SerialName("repositories_url")
    val repositoriesUrl: String? = null,
    val parent: String? = null
)

@Serializable
data class Head(
    val label: String? = null,
    val ref: String? = null,
    val sha: String? = null,
    val user: User? = null,
    val repo: Repo? = null
)


@Serializable
data class Repo(
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    val name: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    val owner: User? = null,
    @SerialName("private")
    val private_field: Boolean? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    val description: String? = null,
    val fork: Boolean? = null,
    val url: String? = null,
    @SerialName("archive_url")
    val archiveUrl: String? = null,
    @SerialName("assignees_url")
    val assigneesUrl: String? = null,
    @SerialName("blobs_url")
    val blobsUrl: String? = null,
    @SerialName("branches_url")
    val branchesUrl: String? = null,
    @SerialName("collaborators_url")
    val collaboratorsUrl: String? = null,
    @SerialName("comments_url")
    val commentsUrl: String? = null,
    @SerialName("commits_url")
    val commitsUrl: String? = null,
    @SerialName("compare_url")
    val compareUrl: String? = null,
    @SerialName("contents_url")
    val contentsUrl: String? = null,
    @SerialName("contributors_url")
    val contributorsUrl: String? = null,
    @SerialName("deployments_url")
    val deploymentsUrl: String? = null,
    @SerialName("downloads_url")
    val downloadsUrl: String? = null,
    @SerialName("events_url")
    val eventsUrl: String? = null,
    @SerialName("forks_url")
    val forksUrl: String? = null,
    @SerialName("git_commits_url")
    val gitCommitsUrl: String? = null,
    @SerialName("git_refs_url")
    val gitRefsUrl: String? = null,
    @SerialName("git_tags_url")
    val gitTagsUrl: String? = null,
    @SerialName("git_url")
    val gitUrl: String? = null,
    @SerialName("issue_comment_url")
    val issueCommentUrl: String? = null,
    @SerialName("issue_events_url")
    val issueEventsUrl: String? = null,
    @SerialName("issues_url")
    val issuesUrl: String? = null,
    @SerialName("keys_url")
    val keysUrl: String? = null,
    @SerialName("labels_url")
    val labelsUrl: String? = null,
    @SerialName("languages_url")
    val languagesUrl: String? = null,
    @SerialName("merges_url")
    val mergesUrl: String? = null,
    @SerialName("milestones_url")
    val milestonesUrl: String? = null,
    @SerialName("notifications_url")
    val notificationsUrl: String? = null,
    @SerialName("pulls_url")
    val pullsUrl: String? = null,
    @SerialName("releases_url")
    val releasesUrl: String? = null,
    @SerialName("ssh_url")
    val sshUrl: String? = null,
    @SerialName("stargazers_url")
    val stargazersUrl: String? = null,
    @SerialName("statuses_url")
    val statusesUrl: String? = null,
    @SerialName("subscribers_url")
    val subscribersUrl: String? = null,
    @SerialName("subscription_url")
    val subscriptionUrl: String? = null,
    @SerialName("tags_url")
    val tagsUrl: String? = null,
    @SerialName("teams_url")
    val teamsUrl: String? = null,
    @SerialName("trees_url")
    val treesUrl: String? = null,
    @SerialName("clone_url")
    val cloneUrl: String? = null,
    @SerialName("mirror_url")
    val mirrorUrl: String? = null,
    @SerialName("hooks_url")
    val hooksUrl: String? = null,
    @SerialName("svn_url")
    val svnUrl: String? = null,
    val homepage: String? = null,
    val language: String? = null,
    @SerialName("forks_count")
    val forksCount: Long? = null,
    @SerialName("stargazers_count")
    val stargazersCount: Long? = null,
    @SerialName("watchers_count")
    val watchersCount: Long? = null,
    val size: Long? = null,
    @SerialName("default_branch")
    val defaultBranch: String? = null,
    @SerialName("open_issues_count")
    val openIssuesCount: Long? = null,
    @SerialName("is_template")
    val isTemplate: Boolean? = null,
    val topics: List<String>? = null,
    @SerialName("has_issues")
    val hasIssues: Boolean? = null,
    @SerialName("has_projects")
    val hasProjects: Boolean? = null,
    @SerialName("has_wiki")
    val hasWiki: Boolean? = null,
    @SerialName("has_pages")
    val hasPages: Boolean? = null,
    @SerialName("has_downloads")
    val hasDownloads: Boolean? = null,
    val archived: Boolean? = null,
    val disabled: Boolean? = null,
    @SerialName("pushed_at")
    val pushedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val permissions: Permissions? = null,
    @SerialName("allow_rebase_merge")
    val allowRebaseMerge: Boolean? = null,
    @SerialName("template_repository")
    val templateRepository: String? = null,
    @SerialName("allow_squash_merge")
    val allowSquashMerge: Boolean? = null,
    @SerialName("allow_merge_commit")
    val allowMergeCommit: Boolean? = null,
    @SerialName("subscribers_count")
    val subscribersCount: Long? = null,
    @SerialName("network_count")
    val networkCount: Long? = null
)


@Serializable
data class Permissions(
    val admin: Boolean? = null,
    val push: Boolean? = null,
    val pull: Boolean? = null
)

@Serializable
data class Base(
    val label: String? = null,
    val ref: String? = null,
    val sha: String? = null,
    val user: User? = null,
    val repo: Repo? = null
)


@Serializable
data class Links(
    val self: Link? = null,
    val html: Link? = null,
    val issue: Link? = null,
    val comments: Link? = null,
    @SerialName("review_comments")
    val reviewComments: Link? = null,
    @SerialName("review_comment")
    val reviewComment: Link? = null,
    val commits: Link? = null,
    val statuses: Link? = null
)

@Serializable
data class Link(
    val href: String? = null
)


@Serializable
data class User(
    val login: String? = null,
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("gravatar_id")
    val gravatarId: String? = null,
    val url: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("followers_url")
    val followersUrl: String? = null,
    @SerialName("following_url")
    val followingUrl: String? = null,
    @SerialName("gists_url")
    val gistsUrl: String? = null,
    @SerialName("starred_url")
    val starredUrl: String? = null,
    @SerialName("subscriptions_url")
    val subscriptionsUrl: String? = null,
    @SerialName("organizations_url")
    val organizationsUrl: String? = null,
    @SerialName("repos_url")
    val reposUrl: String? = null,
    @SerialName("events_url")
    val eventsUrl: String? = null,
    @SerialName("received_events_url")
    val receivedEventsUrl: String? = null,
    val type: String? = null,
    @SerialName("site_admin")
    val siteAdmin: Boolean? = null
)

