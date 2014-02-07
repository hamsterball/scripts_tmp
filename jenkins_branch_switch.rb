require 'jenkins_api_client'
require 'xmlsimple'
require 'getoptlong'

opts = GetoptLong.new(
  [ '--branch', '-b', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--profile', '-p', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--server', '-s', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--username', '-U', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--password', '-P', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--config', '-c', GetoptLong::REQUIRED_ARGUMENT ]
)

  @branch = nil
  @profile = nil
  @server = nil
  @username = nil
  @password = nil
  @config = nil

  opts.each do |opt, arg|
    case opt
      when '--branch'
        @branch = arg
      when '--profile'
        @profile = arg
      when '--server'
        @server = arg
      when '--username'
        @username = arg
      when '--password'
        @password = arg
      when '--config'
        @config = arg
    end
  end

@client = JenkinsApi::Client.new(:server_ip => @server , :username => @username , :password => @password )
xmlconfig = @client.get_config('/job/'+ @config)

xmlhash = XmlSimple.xml_in xmlconfig

def switch_branch hash

  hash['scm'].each do |scm|
    if scm['class'].include? "hudson.plugins.git.GitSCM"
      scm['branches'].each do |branch|
        branch['hudson.plugins.git.BranchSpec'].each do |el|
            el['name'][0] = @branch
        end
      end
    end
  end
end

def switch_profile hash

  hash['builders'].each do |builder|
    builder['hudson.tasks.Maven'].each do |b|
        b['targets'][0] = "clean package -P#{@profile} -U"
    end
  end
end

def build_xml hash
  out = XmlSimple.xml_out hash , {"AnonymousTag" => nil, "KeepRoot" => true, "XmlDeclaration" => "<?xml version='1.0' encoding='UTF-8'?>", "RootName" => "project"}
end

switch_branch xmlhash
switch_profile xmlhash
resxml = build_xml xmlhash

@client.post_config('/job/' + @config + "/config.xml", resxml)
