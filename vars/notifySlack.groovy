import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.model.Actionable;
def notifySlack(String buildStatus = buildstatus, String channel = slackChannel) {

  buildStatus = buildStatus ?: 'SUCCESSFUL'
  channel = channel ?: 'Build-Alerts'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def specificCause = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}] (<${env.RUN_DISPLAY_URL}|Open>) (<${env.RUN_CHANGES_DISPLAY_URL}|  Changes>)' Triggered by ${specificCause.userName}"
  def title = "${env.JOB_NAME} Build: ${env.BUILD_NUMBER}"
  def title_link = "${env.RUN_DISPLAY_URL}"

  def branch_name = sh(returnStdout: true, script: 'git name-rev --name-only HEAD').trim()
  def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
  def author = sh(returnStdout: true, script: "git --no-pager show -s --format='%an'").trim()
  def count = sh(returnStdout: true, script: "git show --stat |tail -n1").trim()
  def commit_files = sh(returnStdout: true, script: "git --no-pager log -m -1 --name-only --pretty='format:' ${commit}")
  def message = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = 'good'
  } else if (buildStatus == 'UNSTABLE') {
    color = 'YELLOW'
    colorCode = 'warning'
  } else {
    color = 'RED'
    colorCode = 'danger'
  }
  JSONObject attachment = new JSONObject();
  attachment.put('author',"jenkins");
  attachment.put('title', title.toString());
  attachment.put('title_link',title_link.toString());
  attachment.put('text', subject.toString());
  attachment.put('fallback', "fallback message");
  attachment.put('color',colorCode);
  attachment.put('mrkdwn_in', ["fields"])
  // JSONObject for branch
  JSONObject branch = new JSONObject();
  branch.put('title', 'Branch');
  branch.put('value', branch_name.toString());
  branch.put('short', true);
  // JSONObject for author
  JSONObject commitAuthor = new JSONObject();
  commitAuthor.put('title', 'Author');
  commitAuthor.put('value', author.toString());
  commitAuthor.put('short', true);
  // JSONObject for count
  JSONObject commitCount = new JSONObject();
  commitCount.put('title', 'Changed Files Count');
  commitCount.put('value', count.toString());
  commitCount.put('short', true);
  // JSONObject for commitMessage
  JSONObject commitMessage = new JSONObject();
  commitMessage.put('title', 'Commit Message');
  commitMessage.put('value', message.toString());
  commitMessage.put('short', false);
    // JSONObject for commitfiles
  JSONObject commitFiles = new JSONObject();
  commitFiles.put('title', 'Commit Files changed');
  commitFiles.put('value', commit_files.toString());
  commitFiles.put('short', false);
  attachment.put('fields', [branch, commitAuthor, commitMessage, commitCount, commitFiles]);
  JSONArray attachments = new JSONArray();
  attachments.add(attachment);
  println attachments.toString()

  // Send notifications
  //https://www.baeldung.com/ops/jenkins-slack-integration
  slackSend (color: colorCode, botUser: true, teamDomain: "Avengers", tokenCredentialId: "Jenkins-slack-integration", username: "jenkins", message: subject, attachments: attachments.toString(), channel: channel)
}
