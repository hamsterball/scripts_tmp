import org.apache.commons.httpclient.*
import org.apache.commons.httpclient.auth.*
import org.apache.commons.httpclient.methods.*
 
/**
 * needs 
 * commons-codec-1.8.jar
 * commons-httpclient-3.1.jar
 * commons-logging-1.1.1.jar * 
 * in classpath
 */
 
/**
 * pattern for jenkins-job = <component>-<branchname>
 * pattern for scm = <base>/<component>/branches/<branchname>
 */
 
// svn-data
def svnBaseUrl = "http://localhost:9000/svn/testrepo"
def svnUsername = "christoph"
def svnPassword = "topfsecret"
 
// jenkins-data
def jenkinsUrl = "http://localhost:10000/"
def jenkinsUsername = "admin"
def jenkinsApiToken = "93edde39fkfffjc752e87dfcc37662"
def jenkinsSecurityRealm = "Benutzer Jenkins"
 
// other-data
def listOfComponents = [
    "testcomponent_1",
    "testcomponent_2",
    "testcomponent_3"
];
def localTmpDir = "./tmp"
def jobConfigTemplate = "config.xml.template"
 
 
main()
 
 
/**
 * - iterate over all components, given in listOfComponents-array
 * - executing "svn list --xml" over branches-directory of each component
 * - fetching the output and iterate over the branches that were found in svn-list-xml
 * - check, if a job for the branch/component-combination exists
 * - create a job, if nothing exists yet
 */
def main(){
    listOfComponents.each(){
        def component = it
        println "found component ${component}"
        svnCommand = "svn list --xml ${svnBaseUrl}/${component}/branches"
        def proc = svnCommand.execute()
        proc.waitFor()
        def xmlOutput = proc.in.text
 
        def tmpDir = new File(localTmpDir)
        if (tmpDir.exists()){
            tmpDir.delete()
        }
        tmpDir.mkdirs()
        def svnInfoFile = new File("${localTmpDir}/${component}_svnInfo.xml")
        svnInfoFile.write(xmlOutput)
 
        def lists = new XmlSlurper().parse(svnInfoFile)
 
        def listOfBranches = lists.list.entry.name
 
        // iterate through branches
        listOfBranches.each(){
            def branchName = it.text()
            println "- found branch '${branchName}'"
            println "--- checking job for '${component}' with branch '${branchName}'"
            if (!jobForBranchExists(component, branchName)){
                createJobForBranch(jenkinsUrl, jenkinsUsername, jenkinsApiToken, jenkinsSecurityRealm, jobConfigTemplate, svnBaseUrl, component, branchName, localTmpDir)
            }
        }
    }
}
 
/**
 * - create a job with several params via Jenkins' remote API
 * - uses a modified version of the given config.xml-template
 * 
 * @param jenkinsUrl
 * @param jenkinsUsername
 * @param jenkinsApiToken
 * @param jenkinsSecurityRealm
 * @param jobConfigTemplate
 * @param svnBaseUrl
 * @param component
 * @param branchName
 * @param localTmpDir
 * @return
 */
def createJobForBranch(String jenkinsUrl, String jenkinsUsername, String jenkinsApiToken, String jenkinsSecurityRealm, String jobConfigTemplate, String svnBaseUrl, String component, String branchName, String localTmpDir){
    def projectName = "${component}-${branchName}"
    def url = new URL(jenkinsUrl)
    def server = url.getHost()
    def port = url.getPort()
 
    def client = new HttpClient()
    client.state.setCredentials(
            new AuthScope( server, port, jenkinsSecurityRealm),
            new UsernamePasswordCredentials( jenkinsUsername, jenkinsApiToken )
            )
 
    client.params.authenticationPreemptive = true
 
    def post = new PostMethod( "${jenkinsUrl}/createItem?name=${projectName}" )
    post.doAuthentication = true
 
    File configXml = createJobConfigXml(jobConfigTemplate, component, branchName, svnBaseUrl, localTmpDir)
    RequestEntity entity = new FileRequestEntity(configXml, "text/xml; charset=UTF-8");
    post.setRequestEntity(entity);
    try {
        int result = client.executeMethod(post)
        if (result != 200) {
            // not nice, but the easiest way
            throw new Exception("http-result-code:" + result);
        }
        println "Return code: ${result}"
        post.responseHeaders.each{ println it.toString().trim() }
        new File("${localTmpDir}/response.html").withWriter{ it << post.getResponseBodyAsString() }
    } finally {
        post.releaseConnection()
    }
    println "--- created Jenkins job '${component}-${branchName}', pointing to url ${svnBaseUrl}/${component}/branches/${branchName}"
}
 
/**
 * create a job-specific config.xml by replacing the content of config-xml-template with correct values for scm-url
 * 
 * @param jobConfigTemplate
 * @param component
 * @param branchName
 * @param svnBaseUrl
 * @param localTmpDir
 * @return the correct config.xml-file
 */
def createJobConfigXml(String jobConfigTemplate, String component, String branchName, String svnBaseUrl, String localTmpDir){
    def jobConfigFile = new File("${localTmpDir}/${component}-${branchName}_config.xml")
    def jobConfigTemplateFile = new File("${jobConfigTemplate}")
 
    def svnUrl = "${svnBaseUrl}/${component}/branches/${branchName}"
 
    def jobConfigTemplateText = jobConfigTemplateFile.text
    def jobConfigText = jobConfigTemplateText.replace("@@@scm-url@@@", svnUrl)
    jobConfigFile.write(jobConfigText)
    return jobConfigFile
}
 
/**
 * looks into Jenkins' jobs-directory, if there is already a job for that branch of this component 
 * @param component
 * @param branchName
 * @return true, if a job exists and false if not
 */
def jobForBranchExists(String component, String branchName){
    def env = System.getenv()
    def jobDirectory = new File(env['JENKINS_HOME'] + "/jobs/${component}-${branchName}")
    if (jobDirectory.exists() ){
        println "--- /jobs/${component}-${branchName} already exists, skipping creation"
        return true
    }
    println "--- /jobs/${component}-${branchName} doesn't exist, starting creation"
    return false
}
