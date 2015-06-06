package net.zomis.duga

import org.grails.web.json.JSONObject

class HookStringification {

    static String repository(json) {
        if (!json.repository) {
            return ''
        }
        return "\\[[$json.repository.full_name]($json.repository.html_url)\\]"
    }

    String format(obj, String str) {
        str.replace('%repository%', repository(obj))
            .replace('%sender%', user(obj.sender))
    }

    static String issue(json) {
        "[**#$json.number: ${json.title.trim()}**]($json.html_url)"
    }

    static String user(json) {
        if (!json) {
            return ''
        }
        return "[**$json.login**]($json.html_url)"
    }

    void ping(List<String> result, JSONObject json) {
        result << format(json, "%repository% Ping: $json.zen")
    }

    String sender(json) {
        "[**$json.sender.login**]($json.sender.html_url)"
    }

    void commit_comment(List<String> result, JSONObject json) {
        String path = json.comment.path
        String commitId = json.comment_commit_id.substring(0, 8)
        String commitLink = "[$commitId]($json.repository.html_url/commit/$json.comment.commit_id)"
        if (path == null || path.isEmpty()) {
            result << format(json, "%repository% %sender% [commented]($json.comment.html_url) on commit $commitLink")
        } else {
            result << format(json, "%repository% %sender% [commented on $json.comment.path]($json.comment.html_url) of commit $commitLink")
        }
    }

    void create(List<String> result, JSONObject json) {
        String refUrl = null;
        switch (json.ref_type) {
            case "branch":
                refUrl = json.repository.html_url + '/tree/' + json.ref
                break;
            case "tag":
                refUrl = json.repository.html_url + "/releases/tag/" + json.ref;
                break;
            case "repository":
                result << format(json, "%repository% %sender% created $json.ref_type")
                return;
        }
        result << format(json, "%repository% %sender% created $json.ref_type [**$json.ref**]($refUrl)")
    }

    void delete(List<String> result, JSONObject json) {
        result << format(json, "%repository% %sender% deleted $json.ref_type **$json.ref**")
    }

    void fork(List<String> result, JSONObject json) {
        result << format(json, "%repository% %sender% forked us into [**$json.forkee.full_name**]($json.forkee.html_url)")
    }

    public String stringify(json, wikiPage) {
        result << format(json, "%repository% %sender% $wikiPage.action wiki page [**${wikiPage.title.trim()}**]($wikiPage.html_url)")
    }

    void issues(List<String> result, JSONObject json) {
        String issue = "[**#$json.issue.number: ${json.issue.title.trim()}**]($json.issue.html_url)"
        String extra = ''
        if (json.assignee) {
            extra = "[**$json.assignee.login**]($json.assignee.html_url)"
        }
        if (json.label) {
            extra = "[**$json.label.name**]($json.repository.html_url/labels/${json.label.name.replace(' ', '%20')})"
        }
        switch (json.action) {
            case 'assigned':
                result << format(json, "%repository% %sender% $json.action $extra to issue $issue")
                break;
            case 'unassigned':
                result << format(json, "%repository% %sender% $json.action $extra from issue $issue")
                break;
            case "labeled":
                result << format(json, "%repository% %sender% added label $extra to issue $issue")
                break;
            case "unlabeled":
                result << format(json, "%repository% %sender% removed label $extra from issue $issue")
                break;
            case "opened":
                result << format(json, "%repository% %sender% opened issue $issue")
                break;
            case "closed":
                result << format(json, "%repository% %sender% closed issue $issue")
                break;
            case "reopened":
                result << format(json, "%repository% %sender% reopened issue $issue")
                break;
            default:
                result << format(json, "%repository% %sender% $json.action issue $issue")
                break;
        }
    }

    void issue_comment(List<String> result, JSONObject json) {
        String issue = issue(json.issue)
        String commentTarget = (json.issue.pull_request == null) ? "issue" : "pull request";
        result << format(json, "%repository% %sender% [commented]($json.comment.html_url) on $commentTarget $issue");
    }

    void member(List<String> result, JSONObject json) {
        result << format(json, "%repository% %sender% $json.action [**$json.member.login**]($json.member.html_url)");
    }

    void pull_request_review_comment(List<String> result, JSONObject json) {
        result << format(json, "%repository% %sender% [commented on **$json.comment.path**]($json.comment.html_url) of pull request ${issue(json.pull_request)}");
    }

    void pull_request(List<String> result, JSONObject json) {
        def head = json.pull_request.head
        def base = json.pull_request.base
        String headText;
        String baseText;
        String pr = issue(json.pull_request)
        String assignee = user(json.assignee)
        if (head.repo.equals(base.repo)) {
            headText = head.ref
            baseText = base.ref
        } else {
            headText = head.repo.full_name + "/" + head.ref
            baseText = base.repo.full_name + "/" + base.ref
        }
        String headStr = "[**$headText**]($head.repo.html_url/tree/$head.ref)"
        String baseStr = "[**$baseText**]($base.repo.html_url/tree/$base.ref)"
        String label = json.label ? "[**$json.label.name**]($json.repository.html_url/labels/${json.label.name.replace(' ', '%20')})" : ''
        switch (json.action) {
            case "assigned":
                result << format(json, "%repository% %sender% assigned $assignee to pull request $pr")
                break;
            case "unassigned":
                result << format(json, "%repository% %sender% unassigned $assignee from pull request $pr")
                break;
            case "labeled":
                result << format(json, "%repository% %sender% added label $label to pull request $pr")
                break;
            case "unlabeled":
                result << format(json, "%repository% %sender% removed label $label from pull request $pr")
                break;
            case "opened":
                if (json.pull_request.body == null || json.pull_request.body.isEmpty()) {
                    result << format(json, "%repository% %sender% created pull request $pr to merge $headStr into $baseStr")
                } else {
                    format(json, "%repository% %sender% created pull request $pr to merge $headStr into $baseStr")
                    list << json.pull_request.body
                }
                break;
            case "closed":
                if (json.pull_request.merged) {
                    result << format(json, "%repository% %sender% merged pull request $pr from $headStr into $baseStr")
                } else {
                    result << format(json, "%repository% %sender% rejected pull request $pr")
                }
                break;
            case "reopened":
                result << format(json, "%repository% %sender% reopened pull request $pr")
                break;
            case "synchronize":
                result << format(json, "%repository% %sender% synchronized pull request $pr")
                break;
            default:
                result << format(json, "%repository% %sender% $json.action pull request $pr")
        }
    }

    void watch(List<String> result, JSONObject json) {
        String action = json.action == 'started' ? 'starred' : json.action;
        result << format(json, "%repository% %sender% $action us")
    }

    void team_add(List<String> result, JSONObject json) {
        String team = "[**$json.team.name**]($json.sender.html_url/$json.team.name)"
        if (json.user == null) {
            result << format(json, "%repository% %sender% added us to team $team")
        }
        else {
            result << format(json, "%repository% %sender% added ${user(json.user)} to team $team")
        }
    }

    public String commit(JSONObject json, commit) {
        String branch = json.ref.replace("refs/heads/", "");
        String committer;
        if (commit.committer != null) {
            committer = commit.committer.user_name;
        }
        else {
            committer = json.pusher_login;
        }
        String commitStr = "[**${commit.id.substring(0, 8)}**]($commit.url)"
        String branchStr = "[**$branch**]($json.repository.url/tree/$branch)"
        if (committer == null) {
            return format(json, "%repository% *Unrecognized author* pushed commit $commitStr to $branchStr")
        } else {
            return format(json, "%repository% [**$committer**](http://github.com/$committer) pushed commit $commitStr to $branchStr")
        }
    }

    public String pushEventSize(JSONObject json, int size) {
        String commitText = (size == 1 ? "commit" : "commits");
        String branch = json.ref.replace("refs/heads/", "");
        return format(json, "%repository% [**$json.pusher.name**](https://github.com/$json.pusher.name) pushed $size $commitText to [**$branch**]($json.repository.url/tree/$branch)")
    }

    List<String> postGithub(String type, JSONObject json) {
        List<String> result = new ArrayList<>()
        result << 'Github event: ' + type
        this."$type"(result, json)
        return result
    }

}